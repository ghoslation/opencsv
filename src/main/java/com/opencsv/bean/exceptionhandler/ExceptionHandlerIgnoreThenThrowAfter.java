package com.opencsv.bean.exceptionhandler;

import com.opencsv.exceptions.CsvException;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * An exception handler that ignores the first x exceptions, then throws any
 * further exceptions.
 * @author Andrew Rucker Jones
 * @since 5.2
 */
final public class ExceptionHandlerIgnoreThenThrowAfter implements CsvExceptionHandler {

    private final AtomicInteger count = new AtomicInteger();
    private final int maxExceptions;

    /**
     * Creates an instance.
     * @param maxExceptions The number of exceptions that will be ignored. Any
     *                      exception handled after this limit will be thrown.
     */
    public ExceptionHandlerIgnoreThenThrowAfter(int maxExceptions) {
        this.maxExceptions = maxExceptions;
    }

    @Override
    public CsvException handleException(CsvException e) throws CsvException {
        if(count.incrementAndGet() > maxExceptions) {
            throw e;
        }
        return null;
    }
}
