package com.opencsv.bean.exceptionhandler;

import com.opencsv.exceptions.CsvException;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>An exception handler that queues the first x exceptions, then throws any
 * further exceptions.</p>
 *
 * <p><b>Note:</b> when testing this on systems with a high number of cores/threads under
 * load we noted discrepancies between the number of exceptions counted and the number
 * exceptions queued.   If it is actually important to see the exceptions thrown then
 * we would heavily recommend you use the single threaded iterator() in CsvToBean
 * and collecting the exceptions yourself.</p>
 *
 * @author Andrew Rucker Jones
 * @since 5.2
 */
final public class ExceptionHandlerQueueThenThrowAfter implements CsvExceptionHandler {

    private final AtomicInteger count = new AtomicInteger();
    private final int maxExceptions;

    /**
     * Creates an instance.
     * @param maxExceptions The number of exceptions that will be queued. Any
     *                      exception handled after this limit will be thrown.
     */
    public ExceptionHandlerQueueThenThrowAfter(int maxExceptions) {
        this.maxExceptions = maxExceptions;
    }

    @Override
    public CsvException handleException(CsvException e) throws CsvException {
        if(count.incrementAndGet() > maxExceptions) {
            throw e;
        }
        return e;
    }
}
