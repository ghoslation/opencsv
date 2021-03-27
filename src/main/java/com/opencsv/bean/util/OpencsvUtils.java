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
package com.opencsv.bean.util;

import com.opencsv.ICSVParser;
import com.opencsv.bean.*;
import com.opencsv.bean.exceptionhandler.CsvExceptionHandler;
import com.opencsv.exceptions.CsvBadConverterException;
import com.opencsv.exceptions.CsvChainedException;
import com.opencsv.exceptions.CsvException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

/**
 * This class is meant to be a collection of general purpose static methods
 * useful in internal processing for opencsv.
 *
 * @author Andrew Rucker Jones
 * @since 3.9
 */
public final class OpencsvUtils {

    /** This class can't be instantiated. */
    private OpencsvUtils() {}

    /**
     * Determines which mapping strategy is appropriate for this bean.
     * The algorithm is:<ol>
     * <li>If annotations {@link CsvBindByPosition},
     * {@link CsvCustomBindByPosition}, {@link CsvBindAndSplitByPosition} or
     * {@link CsvBindAndJoinByPosition} are present,
     * {@link ColumnPositionMappingStrategy} is chosen.</li>
     * <li>Otherwise, {@link HeaderColumnNameMappingStrategy} is chosen. If
     * annotations are present, they will be used, otherwise the field names
     * will be used as the column names.</li></ol>
     *
     * @param <T> The type of the bean for which the mapping strategy is sought
     * @param type The class of the bean for which the mapping strategy is sought
     * @param errorLocale The locale to use for all error messages. If null, the
     *   default locale is used.
     * @param profile The profile to use when configuring bean fields
     * @return A functional mapping strategy for the bean in question
     */
    public static <T> MappingStrategy<T> determineMappingStrategy(
            Class<? extends T> type, Locale errorLocale, String profile) {
        // Check for annotations
        boolean positionAnnotationsPresent = Stream.of(FieldUtils.getAllFields(type)).anyMatch(
                f -> f.isAnnotationPresent(CsvBindByPosition.class)
                || f.isAnnotationPresent(CsvBindAndSplitByPosition.class)
                || f.isAnnotationPresent(CsvBindAndJoinByPosition.class)
                || f.isAnnotationPresent(CsvCustomBindByPosition.class));

        // Set the mapping strategy according to what we've found.
        MappingStrategy<T> mappingStrategy = positionAnnotationsPresent ?
                new ColumnPositionMappingStrategy<>() :
                new HeaderColumnNameMappingStrategy<>();
        mappingStrategy.setErrorLocale(errorLocale);
        mappingStrategy.setProfile(profile);
        mappingStrategy.setType(type);
        return mappingStrategy;
    }

    /**
     * I find it annoying that when I want to queue something in a blocking
     * queue, the thread might be interrupted and I have to try again; this
     * method fixes that.
     * @param <E> The type of the object to be queued
     * @param queue The queue the object should be added to
     * @param object The object to be queued
     * @since 4.0
     */
    public static <E> void queueRefuseToAcceptDefeat(BlockingQueue<E> queue, E object) {
        boolean interrupted = true;
        while(interrupted) {
            try {
                queue.put(object);
                interrupted = false;
            }
            catch(InterruptedException ie) {/* Do nothing. */}
        }
    }

    /**
     * A function to consolidate code common to handling exceptions thrown
     * during reading or writing of CSV files.
     * The proper line number is set for the exception, the exception handler
     * is run, and the exception is queued or thrown as necessary.
     *
     * @param e The exception originally thrown
     * @param lineNumber The line or record number that caused the exception
     * @param exceptionHandler The exception handler
     * @param queue The queue for captured exceptions
     * @since 5.2
     */
    public static synchronized void handleException(
            CsvException e, long lineNumber,
            CsvExceptionHandler exceptionHandler, BlockingQueue<OrderedObject<CsvException>> queue) {
        e.setLineNumber(lineNumber);
        CsvException capturedException = null;
        List<CsvException> exceptionList = e instanceof CsvChainedException ?
                Collections.<CsvException>unmodifiableList(((CsvChainedException)e).getExceptionChain()) :
                Collections.singletonList(e);
        for (CsvException iteratedException : exceptionList) {
            try {
                capturedException = exceptionHandler.handleException(iteratedException);
            } catch (CsvException csve) {
                capturedException = csve;
                throw new RuntimeException(csve);
            } finally {
                if (capturedException != null) {
                    queueRefuseToAcceptDefeat(queue,
                            new OrderedObject<>(lineNumber, capturedException));
                }
            }
        }
    }

    /**
     * Compiles a regular expression into a {@link java.util.regex.Pattern},
     * throwing an exception that is proper in the context of opencsv if the
     * regular expression is not valid, or if it does not have at least one
     * capturing group.
     *
     * @param regex The regular expression to be compiled. May be {@code null}
     *              or an empty string, in which case {@code null} is returned.
     *              Must have at least one capturing group if not {@code null}
     *              or empty.
     * @param regexFlags Flags for compiling the regular expression, as in
     *                   {@link java.util.regex.Pattern#compile(String, int)}.
     * @param callingClass The class from which this method is being called.
     *                     Used for generating helpful exceptions.
     * @param errorLocale  The locale to be used for error messages. If
     *                     {@code null}, the default locale is used.
     * @return A compiled pattern, or {@code null} if the input was null or
     * empty
     * @throws CsvBadConverterException If the regular expression is not empty
     * but invalid or valid but does not have at least one capturing group
     * @since 4.3
     */
    public static Pattern compilePatternAtLeastOneGroup(String regex, int regexFlags, Class<?> callingClass, Locale errorLocale)
            throws CsvBadConverterException {
        Pattern tempPattern = compilePattern(regex, regexFlags, callingClass, errorLocale);
        Locale exceptionLocale = errorLocale == null ? Locale.getDefault() : errorLocale;

        // Verify that the pattern has at least one capture group. This does
        // not appear to be possible without matching a string first.
        if(tempPattern != null) {
            Matcher m = tempPattern.matcher(StringUtils.EMPTY);
            if(m.groupCount() < 1) {
                throw new CsvBadConverterException(callingClass,
                        String.format(ResourceBundle.getBundle(
                                ICSVParser.DEFAULT_BUNDLE_NAME,
                                exceptionLocale).getString("regex.without.capture.group"), regex));
            }
        }

        return tempPattern;
    }

    /**
     * Compiles a regular expression into a {@link java.util.regex.Pattern},
     * throwing an exception that is proper in the context of opencsv if the
     * regular expression is not valid.
     * This method may be used by custom converters if they are required to
     * compile regular expressions that are unknown at compile time.
     *
     * @param regex The regular expression to be compiled. May be {@code null}
     *              or an empty string, in which case {@code null} is returned.
     * @param regexFlags Flags for compiling the regular expression, as in
     *                   {@link java.util.regex.Pattern#compile(String, int)}.
     * @param callingClass The class from which this method is being called.
     *                     Used for generating helpful exceptions.
     * @param errorLocale  The locale to be used for error messages. If
     *                     {@code null}, the default locale is used.
     * @return A compiled pattern, or {@code null} if the input was null or
     * empty
     * @throws CsvBadConverterException If the regular expression is not empty
     * but invalid
     * @since 4.3
     */
    public static Pattern compilePattern(String regex, int regexFlags, Class<?> callingClass, Locale errorLocale)
            throws CsvBadConverterException {
        Pattern tempPattern = null;
        Locale exceptionLocale = errorLocale == null ? Locale.getDefault() : errorLocale;

        // Set up the regular expression for extraction of the value to be
        // converted
        if(StringUtils.isNotEmpty(regex)) {
            try {
                tempPattern = Pattern.compile(regex, regexFlags);
            }
            catch(PatternSyntaxException e) {
                CsvBadConverterException csve = new CsvBadConverterException(
                        callingClass,
                        String.format(ResourceBundle.getBundle(
                                ICSVParser.DEFAULT_BUNDLE_NAME,
                                exceptionLocale).getString("invalid.regex"), regex));
                csve.initCause(e);
                throw csve;
            }
        }
        return tempPattern;
    }

    /**
     * Verifies that the given format string works with one string parameter.
     *
     * @param format A format string for {@link java.lang.String#format(String, Object...)}
     * @param callingClass The class from which this method is being called.
     *                     Used for generating helpful exceptions.
     * @param errorLocale  The locale to be used for error messages. If
     *                     {@code null}, the default locale is used.
     */
    public static void verifyFormatString(String format, Class<?> callingClass, Locale errorLocale) {
        Locale exceptionLocale = errorLocale == null ? Locale.getDefault() : errorLocale;
        try {
            if(StringUtils.isNotEmpty(format)) {
                String okayToIgnore = String.format(format, StringUtils.SPACE);
            }
        }
        catch(IllegalFormatException e) {
            CsvBadConverterException csve = new CsvBadConverterException(
                    callingClass,
                    String.format(ResourceBundle.getBundle(
                            ICSVParser.DEFAULT_BUNDLE_NAME,
                            exceptionLocale).getString("invalid.one.parameter.format.string"), format));
            csve.initCause(e);
            throw csve;
        }
    }
}
