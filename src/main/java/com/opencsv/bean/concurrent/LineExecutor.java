package com.opencsv.bean.concurrent;

import com.opencsv.ICSVParser;
import com.opencsv.bean.BeanVerifier;
import com.opencsv.bean.CsvToBeanFilter;
import com.opencsv.bean.MappingStrategy;
import com.opencsv.bean.exceptionhandler.CsvExceptionHandler;
import com.opencsv.exceptions.CsvMalformedLineException;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.RejectedExecutionException;

/**
 * A specific derivative of {@link IntolerantThreadPoolExecutor} intended for
 * submitting lines of input to be converted to beans.
 *
 * @param <T> The type of the bean being converted to
 * @author Andrew Rucker Jones
 * @since 5.0
 */
public class LineExecutor<T> extends IntolerantThreadPoolExecutor<T> {

    private final CompleteFileReader<T> completeFileReader;

    /**
     * The only constructor available for this class.
     * @param orderedResults Whether order should be preserved in the results
     * @param errorLocale The locale to use for error messages
     * @param completeFileReader The thread that reads lines of input and feeds the
     *                   results to this Executor
     */
    public LineExecutor(boolean orderedResults, Locale errorLocale, CompleteFileReader<T> completeFileReader) {
        super(orderedResults, errorLocale);
        this.completeFileReader = completeFileReader;
    }

    @Override
    public void prepare() {
        Thread readerThread = new Thread(completeFileReader);
        completeFileReader.setExecutor(this);
        super.prepare();
        readerThread.start();
    }

    @Override
    protected void checkExceptions() {
        Throwable t = completeFileReader.getTerminalException();

        // RejectedExecutionException indicates a problem encountered when
        // submitting a line for processing by the Executor, specifically that
        // the Executor has shut down, but the base class will take care of
        // errors that would cause the Executor to shut down.
        if(t != null && !(t instanceof RejectedExecutionException)) {
            shutdownNow();
            if(t instanceof CsvMalformedLineException) {
                // Exception during parsing. Always unrecoverable.
                CsvMalformedLineException cmle = (CsvMalformedLineException) t;
                throw new RuntimeException(String.format(ResourceBundle.getBundle(ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale).getString("parsing.error.full"),
                        cmle.getLineNumber(), cmle.getContext()), cmle);
            }
            throw new RuntimeException(String.format(ResourceBundle.getBundle(ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale).getString("parsing.error.full"),
                    completeFileReader.getLineProcessed(), Arrays.toString(completeFileReader.getLine())), t);
        }
        super.checkExceptions();
    }

    /**
     * Submit one record for conversion to a bean.
     *
     * @param lineNumber Which record in the input file is being processed
     * @param mapper The mapping strategy to be used
     * @param filter A filter to remove beans from the running, if necessary.
     *   May be null.
     * @param verifiers The list of verifiers to run on beans after creation
     * @param line The line of input to be transformed into a bean
     * @param exceptionHandler The handler for exceptions thrown during record
     *                         processing
     */
    public void submitLine(
            long lineNumber, MappingStrategy<? extends T> mapper, CsvToBeanFilter filter,
            List<BeanVerifier<T>> verifiers, String[] line,
            CsvExceptionHandler exceptionHandler) {
        if (accumulateThread != null) {
            expectedRecords.add(lineNumber);
        }
        try {
            execute(new ProcessCsvLine<>(
                    lineNumber, mapper, filter, verifiers, line,
                    resultQueue, thrownExceptionsQueue,
                    expectedRecords, exceptionHandler));
        } catch (Exception e) {
            if(accumulateThread != null) {
                expectedRecords.remove(lineNumber);
                accumulateThread.setMustStop(true);
            }
            throw e;
        }
    }
}
