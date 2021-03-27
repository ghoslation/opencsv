/*
 * Copyright 2017 Andrew Rucker Jones.
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
package com.opencsv.bean.concurrent;

import com.opencsv.bean.MappingStrategy;
import com.opencsv.bean.exceptionhandler.CsvExceptionHandler;
import com.opencsv.bean.util.OpencsvUtils;
import com.opencsv.bean.util.OrderedObject;
import com.opencsv.exceptions.CsvChainedException;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvFieldAssignmentException;
import com.opencsv.exceptions.CsvRuntimeException;

import java.util.SortedSet;
import java.util.concurrent.BlockingQueue;

/**
 * A class for converting one bean into its string representation for writing to
 * an output.
 * @param <T> The type of the bean to be processed
 * @since 4.0
 * @author Andrew Rucker Jones
 */
public class ProcessCsvBean<T> implements Runnable {
    
    private final long lineNumber;
    private final MappingStrategy<T> mappingStrategy;
    private final T bean;
    private final BlockingQueue<OrderedObject<String[]>> resultantLineQueue;
    private final BlockingQueue<OrderedObject<CsvException>> thrownExceptionsQueue;
    private final SortedSet<Long> expectedRecords;
    private final CsvExceptionHandler exceptionHandler;
    
    /**
     * The only constructor for creating a line of CSV output out of a bean.
     * @param lineNumber Which record in the output file is being processed
     * @param mappingStrategy The mapping strategy to be used
     * @param bean The bean to be transformed into a line of output
     * @param resultantLineQueue A queue in which to place the line created
     * @param thrownExceptionsQueue A queue in which to place a thrown
     *   exception, if one is thrown
     * @param expectedRecords A list of outstanding record numbers so gaps
     *                        in ordering due to filtered input or exceptions
     *                        while converting can be detected.
     * @param exceptionHandler The handler for exceptions thrown during record
     *                         processing
     */
    public ProcessCsvBean(long lineNumber, MappingStrategy<T> mappingStrategy,
            T bean, BlockingQueue<OrderedObject<String[]>> resultantLineQueue,
            BlockingQueue<OrderedObject<CsvException>> thrownExceptionsQueue,
            SortedSet<Long> expectedRecords, CsvExceptionHandler exceptionHandler) {
        this.lineNumber = lineNumber;
        this.mappingStrategy = mappingStrategy;
        this.bean = bean;
        this.resultantLineQueue = resultantLineQueue;
        this.thrownExceptionsQueue = thrownExceptionsQueue;
        this.expectedRecords = expectedRecords;
        this.exceptionHandler = exceptionHandler;
    }
    
    @Override
    public void run() {
        try {
            OpencsvUtils.queueRefuseToAcceptDefeat(resultantLineQueue,
                    new OrderedObject<>(lineNumber, mappingStrategy.transmuteBean(bean)));
        }
        catch (CsvFieldAssignmentException | CsvChainedException e) {
            expectedRecords.remove(lineNumber);
            OpencsvUtils.handleException(e, lineNumber, exceptionHandler, thrownExceptionsQueue);
        }
        catch(CsvRuntimeException csvre) {
            expectedRecords.remove(lineNumber);
            // Rethrowing exception here because I do not want the CsvRuntimeException caught and rewrapped in the catch below.
            throw csvre;
        }
        catch(Exception t) {
            expectedRecords.remove(lineNumber);
            throw new RuntimeException(t);
        }
    }
    
}
