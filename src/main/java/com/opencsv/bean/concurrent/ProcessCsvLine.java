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

import com.opencsv.bean.BeanVerifier;
import com.opencsv.bean.CsvToBeanFilter;
import com.opencsv.bean.MappingStrategy;
import com.opencsv.bean.exceptionhandler.CsvExceptionHandler;
import com.opencsv.bean.util.OpencsvUtils;
import com.opencsv.bean.util.OrderedObject;
import com.opencsv.exceptions.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.util.*;
import java.util.concurrent.BlockingQueue;

/**
 * A class that encapsulates the job of creating a bean from a line of CSV input
 * and making it possible to run those jobs in parallel.
 * @param <T> The type of the bean being created
 * @author Andrew Rucker Jones
 * @since 4.0
 */
public class ProcessCsvLine<T> implements Runnable {
    private final long lineNumber;
    private final MappingStrategy<? extends T> mapper;
    private final CsvToBeanFilter filter;
    private final List<BeanVerifier<T>> verifiers;
    private final String[] line;
    private final BlockingQueue<OrderedObject<T>> resultantBeanQueue;
    private final BlockingQueue<OrderedObject<CsvException>> thrownExceptionsQueue;
    private final SortedSet<Long> expectedRecords;
    private final CsvExceptionHandler exceptionHandler;

    /**
     * The only constructor for creating a bean out of a line of input.
     * @param lineNumber Which record in the input file is being processed
     * @param mapper The mapping strategy to be used
     * @param filter A filter to remove beans from the running, if necessary.
     *   May be null.
     * @param verifiers The list of verifiers to run on beans after creation
     * @param line The line of input to be transformed into a bean
     * @param resultantBeanQueue A queue in which to place the bean created
     * @param thrownExceptionsQueue A queue in which to place a thrown
     *   exception, if one is thrown
     * @param expectedRecords A list of outstanding record numbers so gaps
     *                        in ordering due to filtered input or exceptions
     *                        while converting can be detected.
     * @param exceptionHandler The handler for exceptions thrown during record
     *                         processing
     */
    public ProcessCsvLine(
            long lineNumber, MappingStrategy<? extends T> mapper, CsvToBeanFilter filter,
            List<BeanVerifier<T>> verifiers, String[] line,
            BlockingQueue<OrderedObject<T>> resultantBeanQueue,
            BlockingQueue<OrderedObject<CsvException>> thrownExceptionsQueue,
            SortedSet<Long> expectedRecords, CsvExceptionHandler exceptionHandler) {
        this.lineNumber = lineNumber;
        this.mapper = mapper;
        this.filter = filter;
        this.verifiers = ObjectUtils.defaultIfNull(new ArrayList<>(verifiers), Collections.emptyList());
        this.line = ArrayUtils.clone(line);
        this.resultantBeanQueue = resultantBeanQueue;
        this.thrownExceptionsQueue = thrownExceptionsQueue;
        this.expectedRecords = expectedRecords;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void run() {
        try {
            if (filter == null || filter.allowLine(line)) {
                T obj = processLine();
                ListIterator<BeanVerifier<T>> verifierList = verifiers.listIterator();
                boolean keep = true;
                while(keep && verifierList.hasNext()) {
                    keep = verifierList.next().verifyBean(obj);
                }
                if (keep) {
                    OpencsvUtils.queueRefuseToAcceptDefeat(
                            resultantBeanQueue,
                            new OrderedObject<>(lineNumber, obj));
                }
                else {
                    expectedRecords.remove(lineNumber);
                }
            }
            else {
                expectedRecords.remove(lineNumber);
            }
        } catch (CsvException e) {
            expectedRecords.remove(lineNumber);
            e.setLine(line);
            OpencsvUtils.handleException(e, lineNumber, exceptionHandler, thrownExceptionsQueue);
        } catch (Exception e) {
            expectedRecords.remove(lineNumber);
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a single object from a line from the CSV file.
     * @return Object containing the values.
     * @throws CsvBeanIntrospectionException Thrown on error creating bean.
     * @throws CsvBadConverterException If a custom converter cannot be
     *   initialized properly
     * @throws CsvFieldAssignmentException A more specific subclass of this
     *   exception is thrown for any problem decoding and assigning a field
     *   of the input to a bean field
     * @throws CsvChainedException If multiple exceptions are thrown for the
     * same input line
     */
    private T processLine()
            throws CsvBeanIntrospectionException,
            CsvBadConverterException, CsvFieldAssignmentException,
            CsvChainedException {
        return mapper.populateNewBean(line);
    }
}
