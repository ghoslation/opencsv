package com.opencsv.bean.exceptionhandler;

import com.opencsv.exceptions.CsvException;

/**
 * An exception handler that always ignores exceptions raised.
 *
 * @author Andrew Rucker Jones
 * @since 5.2
 */
final public class ExceptionHandlerIgnore implements CsvExceptionHandler {

    /**
     * Default Constructor.
     */
    public ExceptionHandlerIgnore() {
    }

    @Override
    public CsvException handleException(CsvException e) {
        return null;
    }
}
