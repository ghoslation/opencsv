package com.opencsv.bean.concurrent;

import com.opencsv.bean.MappingStrategy;
import com.opencsv.bean.exceptionhandler.CsvExceptionHandler;

import java.util.Locale;

/**
 * A specific derivative of {@link IntolerantThreadPoolExecutor} intended for
 * submitting beans to be converted to {@link java.lang.String}s for writing.
 *
 * @param <T> The type of the bean being converted
 * @author Andrew Rucker Jones
 * @since 5.0
 */
public class BeanExecutor<T> extends IntolerantThreadPoolExecutor<String[]> {

    /**
     * The only constructor available for this class.
     * @param orderedResults Whether order should be preserved in the results
     * @param errorLocale The locale to use for error messages
     */
    public BeanExecutor(boolean orderedResults, Locale errorLocale) {
        super(orderedResults, errorLocale);
    }

    /**
     * Submit one bean for conversion.
     *
     * @param lineNumber Which record in the output file is being processed
     * @param mappingStrategy The mapping strategy to be used
     * @param bean The bean to be transformed into a line of output
     * @param exceptionHandler The handler for exceptions thrown during record
     *                         processing
     */
    public void submitBean(
            long lineNumber, MappingStrategy<T> mappingStrategy,
            T bean, CsvExceptionHandler exceptionHandler) {
        if (accumulateThread != null) {
            expectedRecords.add(lineNumber);
        }
        try {
            execute(new ProcessCsvBean<>(lineNumber, mappingStrategy, bean, resultQueue,
                    thrownExceptionsQueue, expectedRecords, exceptionHandler));
        } catch (Exception e) {
            if(accumulateThread != null) {
                expectedRecords.remove(lineNumber);
                accumulateThread.setMustStop(true);
            }
            throw e;
        }
    }
}
