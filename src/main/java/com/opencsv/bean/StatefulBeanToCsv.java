/*
 * Copyright 2016 Andrew Rucker Jones.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.opencsv.bean;

import com.opencsv.CSVWriter;
import com.opencsv.ICSVParser;
import com.opencsv.ICSVWriter;
import com.opencsv.bean.concurrent.BeanExecutor;
import com.opencsv.bean.concurrent.ProcessCsvBean;
import com.opencsv.bean.exceptionhandler.CsvExceptionHandler;
import com.opencsv.bean.exceptionhandler.ExceptionHandlerThrow;
import com.opencsv.bean.util.OpencsvUtils;
import com.opencsv.bean.util.OrderedObject;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import com.opencsv.exceptions.CsvRuntimeException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.iterators.PeekingIterator;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.Writer;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This class writes beans out in CSV format to a {@link java.io.Writer},
 * keeping state information and making an intelligent guess at the mapping
 * strategy to be applied.
 * <p>This class implements multi-threading on writing more than one bean, so
 * there should be no need to use it across threads in an application. As such,
 * it is not thread-safe.</p>
 *
 * @param <T> Type of the bean to be written
 * @author Andrew Rucker Jones
 * @see OpencsvUtils#determineMappingStrategy(java.lang.Class, java.util.Locale, java.lang.String)
 * @since 3.9
 */
public class StatefulBeanToCsv<T> {
    private static final char NO_CHARACTER = '\0';
    /**
     * The beans being written are counted in the order they are written.
     */
    private int lineNumber = 0;

    private final char separator;
    private final char quotechar;
    private final char escapechar;
    private final String lineEnd;
    private boolean headerWritten = false;
    private MappingStrategy<T> mappingStrategy;
    private final Writer writer;
    private ICSVWriter csvwriter;
    private final CsvExceptionHandler exceptionHandler;
    private List<CsvException> capturedExceptions = new ArrayList<>();
    private boolean orderedResults = true;
    private BeanExecutor<T> executor = null;
    private Locale errorLocale = Locale.getDefault();
    private final boolean applyQuotesToAll;
    private final MultiValuedMap<Class<?>, Field> ignoredFields;
    private final String profile;

    /**
     * Constructor used when supplying a Writer instead of a CsvWriter class.
     * It is defined as package protected to ensure that {@link StatefulBeanToCsvBuilder} is always used.
     *
     * @param escapechar       The escape character to use when writing a CSV file
     * @param lineEnd          The line ending to use when writing a CSV file
     * @param mappingStrategy  The mapping strategy to use when writing a CSV file
     * @param quotechar        The quote character to use when writing a CSV file
     * @param separator        The field separator to use when writing a CSV file
     * @param exceptionHandler Determines the exception handling behavior
     * @param writer           A {@link java.io.Writer} for writing the beans as a CSV to
     * @param applyQuotesToAll Whether all output fields should be quoted
     * @param ignoredFields The fields to ignore during processing. May be {@code null}.
     * @param profile The profile to use when configuring how to interpret bean fields
     */
    StatefulBeanToCsv(char escapechar, String lineEnd,
                      MappingStrategy<T> mappingStrategy, char quotechar, char separator,
                      CsvExceptionHandler exceptionHandler, Writer writer, boolean applyQuotesToAll,
                      MultiValuedMap<Class<?>, Field> ignoredFields, String profile) {
        this.escapechar = escapechar;
        this.lineEnd = lineEnd;
        this.mappingStrategy = mappingStrategy;
        this.quotechar = quotechar;
        this.separator = separator;
        this.exceptionHandler = exceptionHandler;
        this.writer = writer;
        this.applyQuotesToAll = applyQuotesToAll;
        this.ignoredFields = ignoredFields;
        this.profile = StringUtils.defaultString(profile);
    }

    /**
     * Constructor used to allow building of a {@link com.opencsv.bean.StatefulBeanToCsv}
     * with a user-supplied {@link com.opencsv.ICSVWriter} class.
     *
     * @param mappingStrategy  The mapping strategy to use when writing a CSV file
     * @param exceptionHandler Determines the exception handling behavior
     * @param applyQuotesToAll Whether all output fields should be quoted
     * @param csvWriter        An user-supplied {@link com.opencsv.ICSVWriter} for writing beans to a CSV output
     * @param ignoredFields The fields to ignore during processing. May be {@code null}.
     * @param profile The profile to use when configuring how to interpret bean fields
     */
    public StatefulBeanToCsv(MappingStrategy<T> mappingStrategy,
                             CsvExceptionHandler exceptionHandler, boolean applyQuotesToAll,
                             ICSVWriter csvWriter,
                             MultiValuedMap<Class<?>, Field> ignoredFields, String profile) {
        this.mappingStrategy = mappingStrategy;
        this.exceptionHandler = exceptionHandler;
        this.applyQuotesToAll = applyQuotesToAll;
        this.csvwriter = csvWriter;

        this.escapechar = NO_CHARACTER;
        this.lineEnd = "";
        this.quotechar = NO_CHARACTER;
        this.separator = NO_CHARACTER;
        this.writer = null;
        this.ignoredFields = ignoredFields;
        this.profile = StringUtils.defaultString(profile);
    }

    /**
     * Custodial tasks that must be performed before beans are written to a CSV
     * destination for the first time.
     *
     * @param bean Any bean to be written. Used to determine the mapping
     *             strategy automatically. The bean itself is not written to the output by
     *             this method.
     * @throws CsvRequiredFieldEmptyException If a required header is missing
     *                                        while attempting to write. Since every other header is hard-wired
     *                                        through the bean fields and their associated annotations, this can only
     *                                        happen with multi-valued fields.
     */
    private void beforeFirstWrite(T bean) throws CsvRequiredFieldEmptyException {

        // Determine mapping strategy
        if (mappingStrategy == null) {
            mappingStrategy = OpencsvUtils.determineMappingStrategy(
                    (Class<T>) bean.getClass(), errorLocale, profile);
        }

        // Ignore fields. It's possible the mapping strategy has already been
        // primed, so only pass on our data if the user actually gave us
        // something.
        if(!ignoredFields.isEmpty()) {
            mappingStrategy.ignoreFields(ignoredFields);
        }

        // Build CSVWriter
        if (csvwriter == null) {
            csvwriter = new CSVWriter(writer, separator, quotechar, escapechar, lineEnd);
        }

        // Write the header
        String[] header = mappingStrategy.generateHeader(bean);
        if (header.length > 0) {
            csvwriter.writeNext(header, applyQuotesToAll);
        }
        headerWritten = true;
    }


    /**
     * Writes a bean out to the {@link java.io.Writer} provided to the
     * constructor.
     *
     * @param bean A bean to be written to a CSV destination
     * @throws CsvDataTypeMismatchException   If a field of the bean is
     *                                        annotated improperly or an unsupported data type is supposed to be
     *                                        written
     * @throws CsvRequiredFieldEmptyException If a field is marked as required,
     *                                        but the source is null
     */
    public void write(T bean) throws CsvDataTypeMismatchException,
            CsvRequiredFieldEmptyException {

        // Write header
        if (bean != null) {
            if (!headerWritten) {
                beforeFirstWrite(bean);
            }

            // Process the bean
            BlockingQueue<OrderedObject<String[]>> resultantLineQueue = new ArrayBlockingQueue<>(1);
            BlockingQueue<OrderedObject<CsvException>> thrownExceptionsQueue = new LinkedBlockingQueue<>();
            ProcessCsvBean<T> proc = new ProcessCsvBean<>(++lineNumber,
                    mappingStrategy, bean, resultantLineQueue,
                    thrownExceptionsQueue, new TreeSet<>(), exceptionHandler);
            try {
                proc.run();
            } catch (RuntimeException re) {
                if (re.getCause() != null) {
                    if (re.getCause() instanceof CsvRuntimeException) {
                        // Can't currently happen, but who knows what might be
                        // in the future? I'm certain we wouldn't want to wrap
                        // these in another RuntimeException.
                        throw (CsvRuntimeException) re.getCause();
                    }
                    if (re.getCause() instanceof CsvDataTypeMismatchException) {
                        throw (CsvDataTypeMismatchException) re.getCause();
                    }
                    if (re.getCause() instanceof CsvRequiredFieldEmptyException) {
                        throw (CsvRequiredFieldEmptyException) re.getCause();
                    }
                }
                throw re;
            }

            // Write out the result
            if (!thrownExceptionsQueue.isEmpty()) {
                OrderedObject<CsvException> o = thrownExceptionsQueue.poll();
                while (o != null && o.getElement() != null) {
                    capturedExceptions.add(o.getElement());
                    o = thrownExceptionsQueue.poll();
                }
            } else {
                // No exception, so there really must always be a string
                OrderedObject<String[]> result = resultantLineQueue.poll();
                if (result != null && result.getElement() != null) {
                    csvwriter.writeNext(result.getElement(), applyQuotesToAll);
                }
            }
        }
    }

    private void submitAllLines(Iterator<T> beans) throws InterruptedException {
        while (beans.hasNext()) {
            T bean = beans.next();
            if (bean != null) {
                executor.submitBean(++lineNumber, mappingStrategy, bean, exceptionHandler);
            }
        }
        executor.complete();
    }

    /**
     * Writes a list of beans out to the {@link java.io.Writer} provided to the
     * constructor.
     *
     * @param beans A list of beans to be written to a CSV destination
     * @throws CsvDataTypeMismatchException   If a field of the beans is
     *                                        annotated improperly or an unsupported data type is supposed to be
     *                                        written
     * @throws CsvRequiredFieldEmptyException If a field is marked as required,
     *                                        but the source is null
     */
    public void write(List<T> beans) throws CsvDataTypeMismatchException,
            CsvRequiredFieldEmptyException {
        if (CollectionUtils.isNotEmpty(beans)) {
            write(beans.iterator());
        }
    }

    /**
     * Writes an iterator of beans out to the {@link java.io.Writer} provided to the
     * constructor.
     *
     * @param iBeans An iterator of beans to be written to a CSV destination
     * @throws CsvDataTypeMismatchException   If a field of the beans is annotated improperly or an unsupported
     *                                        data type is supposed to be written
     * @throws CsvRequiredFieldEmptyException If a field is marked as required, but the source is null
     */
    public void write(Iterator<T> iBeans) throws CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {

        PeekingIterator<T> beans = new PeekingIterator<>(iBeans);
        T firstBean = beans.peek();

        if (!beans.hasNext()) {
            return;
        }

        // Write header
        if (!headerWritten) {
            beforeFirstWrite(firstBean);
        }

        executor = new BeanExecutor<>(orderedResults, errorLocale);
        executor.prepare();

        // Process the beans
        try {
            submitAllLines(beans);
        } catch (RejectedExecutionException e) {
            // An exception in one of the bean writing threads prompted the
            // executor service to shutdown before we were done.
            if (executor.getTerminalException() instanceof RuntimeException) {
                throw (RuntimeException) executor.getTerminalException();
            }
            if (executor.getTerminalException() instanceof CsvDataTypeMismatchException) {
                throw (CsvDataTypeMismatchException) executor.getTerminalException();
            }
            if (executor.getTerminalException() instanceof CsvRequiredFieldEmptyException) {
                throw (CsvRequiredFieldEmptyException) executor
                        .getTerminalException();
            }
            throw new RuntimeException(ResourceBundle.getBundle(ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale)
                    .getString("error.writing.beans"), executor.getTerminalException());
        } catch (Exception e) {
            // Exception during parsing. Always unrecoverable.
            // I can't find a way to create this condition in the current
            // code, but we must have a catch-all clause.
            executor.shutdownNow();
            if (executor.getTerminalException() instanceof RuntimeException) {
                throw (RuntimeException) executor.getTerminalException();
            }
            throw new RuntimeException(ResourceBundle.getBundle(ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale)
                    .getString("error.writing.beans"), e);
        }
        finally {
            capturedExceptions.addAll(executor.getCapturedExceptions());
        }

        StreamSupport.stream(executor, false)
                .forEach(l -> csvwriter.writeNext(l, applyQuotesToAll));
    }

    /**
     * Writes a stream of beans out to the {@link java.io.Writer} provided to the
     * constructor.
     *
     * @param beans A stream of beans to be written to a CSV destination
     * @throws CsvDataTypeMismatchException   If a field of the beans is annotated improperly or an unsupported
     *                                        data type is supposed to be written
     * @throws CsvRequiredFieldEmptyException If a field is marked as required, but the source is null
     */
    public void write(Stream<T> beans) throws CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {
        write(beans.iterator());
    }

    /**
     * Sets whether or not results must be written in the same order in which
     * they appear in the list of beans provided as input.
     * The default is that order is preserved. If your data do not need to be
     * ordered, you can get a slight performance boost by setting
     * {@code orderedResults} to {@code false}. The lack of ordering then also
     * applies to any captured exceptions, if you have chosen not to have
     * exceptions thrown.
     *
     * @param orderedResults Whether or not the lines written are in the same
     *                       order they appeared in the input
     * @since 4.0
     */
    public void setOrderedResults(boolean orderedResults) {
        this.orderedResults = orderedResults;
    }

    /**
     * @return Whether or not exceptions are thrown. If they are not thrown,
     * they are captured and returned later via {@link #getCapturedExceptions()}.
     * @deprecated There is simply no need for this method.
     */
    @Deprecated
    public boolean isThrowExceptions() {
        return exceptionHandler instanceof ExceptionHandlerThrow;
    }

    /**
     * Any exceptions captured during writing of beans to a CSV destination can
     * be retrieved through this method.
     * <p><em>Reads from the list are destructive!</em> Calling this method will
     * clear the list of captured exceptions. However, calling
     * {@link #write(java.util.List)} or {@link #write(java.lang.Object)}
     * multiple times with no intervening call to this method will not clear the
     * list of captured exceptions, but rather add to it if further exceptions
     * are thrown.</p>
     *
     * @return A list of exceptions that would have been thrown during any and
     * all read operations since the last call to this method
     */
    public List<CsvException> getCapturedExceptions() {
        List<CsvException> intermediate = capturedExceptions;
        capturedExceptions = new ArrayList<>();
        intermediate.sort(Comparator.comparingLong(CsvException::getLineNumber));
        return intermediate;
    }

    /**
     * Sets the locale for all error messages.
     *
     * @param errorLocale Locale for error messages. If null, the default locale
     *                    is used.
     * @since 4.0
     */
    public void setErrorLocale(Locale errorLocale) {
        this.errorLocale = ObjectUtils.defaultIfNull(errorLocale, Locale.getDefault());
    }
}
