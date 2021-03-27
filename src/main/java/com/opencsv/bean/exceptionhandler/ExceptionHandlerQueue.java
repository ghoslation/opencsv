package com.opencsv.bean.exceptionhandler;

import com.opencsv.exceptions.CsvException;

/**
 * An exception handler that always queues exceptions raised.
 *
 * @author Andrew Rucker Jones
 * @since 5.2
 */
final public class ExceptionHandlerQueue implements CsvExceptionHandler {

    /**
     * Default Constructor.
     */
    public ExceptionHandlerQueue() {
    }

    @Override
    public CsvException handleException(CsvException e) {
        return e;
    }
}
