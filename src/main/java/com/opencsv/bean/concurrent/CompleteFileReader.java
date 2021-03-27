package com.opencsv.bean.concurrent;

import com.opencsv.CSVReader;
import com.opencsv.bean.BeanVerifier;
import com.opencsv.bean.CsvToBeanFilter;
import com.opencsv.bean.MappingStrategy;
import com.opencsv.bean.exceptionhandler.CsvExceptionHandler;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Collections;
import java.util.List;

/**
 * Implements a separate thread for reading input and siphoning it to a
 * {@link LineExecutor}.
 * @param <T> The type of bean being created
 * @author Andrew Rucker Jones
 * @since 5.2
 */
public class CompleteFileReader<T> extends SingleLineReader implements Runnable {

    /** Filter to be applied to the input. */
    private final CsvToBeanFilter filter;

    /** The mapping strategy in use. */
    private final MappingStrategy<? extends T> mappingStrategy;

    /** Whether exceptions in processing should be thrown or collected. */
    private final CsvExceptionHandler exceptionHandler;

    /** Verifiers to be applied to the beans created. */
    private final List<BeanVerifier<T>> verifiers;

    /** Counts how many records have been read from the input. */
    private long lineProcessed;

    /** The exception that brought execution to a grinding halt. */
    private Throwable terminalException;

    /** The executor that takes lines of input and converts them to beans. */
    private LineExecutor<T> executor;

    /**
     *
     * @param csvReader The {@link CSVReader} from which input is read
     * @param filter Filter to be applied to the input
     * @param ignoreEmptyLines Whether empty lines of input should be ignored
     * @param mappingStrategy The mapping strategy in use
     * @param exceptionHandler Determines the exception handling behavior
     * @param verifiers Verifiers to be applied to the beans created
     */
    public CompleteFileReader(CSVReader csvReader, CsvToBeanFilter filter,
                              boolean ignoreEmptyLines,
                              MappingStrategy<? extends T> mappingStrategy,
                              CsvExceptionHandler exceptionHandler,
                              List<BeanVerifier<T>> verifiers) {
        super(csvReader, ignoreEmptyLines);
        this.filter = filter;
        this.mappingStrategy = mappingStrategy;
        this.exceptionHandler = exceptionHandler;
        this.verifiers = ObjectUtils.defaultIfNull(verifiers, Collections.<BeanVerifier<T>>emptyList());
    }

    /**
     * @return The exception that brought execution to a halt
     */
    public Throwable getTerminalException() {return terminalException;}

    /**
     * @return How many lines have been processed thus far
     */
    public long getLineProcessed() {return lineProcessed;}

    /**
     * Sets the executor that will convert text input to bean output.
     * @param executor The executor to use
     */
    public void setExecutor(LineExecutor<T> executor) {
        if(this.executor == null) {
            this.executor = executor;
        }
    }

    /**
     * Runs a nice, tight loop to simply read input and submit for conversion.
     */
    @Override
    public void run() {
        // Parse through each line of the file
        try {
            while (null != readNextLine()) {
                lineProcessed = csvReader.getLinesRead();
                executor.submitLine(lineProcessed, mappingStrategy, filter,
                        verifiers, line, exceptionHandler);
            }

            // Since only this thread knows when reading is over, it is responsible
            // for telling the executor it's finished.
            executor.complete();
        } catch(Exception e) {
            terminalException = e;
        }
    }
}
