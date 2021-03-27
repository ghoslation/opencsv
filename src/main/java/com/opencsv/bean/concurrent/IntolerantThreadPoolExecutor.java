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

import com.opencsv.ICSVParser;
import com.opencsv.bean.util.OrderedObject;
import com.opencsv.exceptions.CsvException;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * This ThreadPoolExecutor automatically shuts down on any failed thread.
 * <p>This is the historically established precedent for dealing with input errors
 * in opencsv. This implementation expects all uncaught exceptions from its
 * threads to be wrapped in a {@link java.lang.RuntimeException}. The number of
 * threads in the pool is fixed.</p>
 * <p>It is not intended for this executor to be instantiated and receive jobs
 * directly. There are function-specific derived classes for that purpose.</p>
 * <p>This executor adds significant logic to the basic idea of an
 * {@link java.util.concurrent.Executor}, and thus must be used differently
 * from other executors. Usage follows this pattern:
 * <ol><li>{@link #prepare()}</li>
 * <li>Submit tasks. This is not intended to be done directly to this class, but
 * rather to one of the submission methods of the derived classes.</li>
 * <li>{@link #complete()}</li>
 * <li>The results are had by creating a {@link java.util.stream.Stream} out of
 * the executor itself. This is most easily done with
 * {@link java.util.stream.StreamSupport#stream(Spliterator, boolean)}</li>
 * <li>Possibly {@link #getCapturedExceptions()}</li></ol></p>
 * <p>The execution structure of this class is:
 * <ol><li>The main thread (outside of this executor) parses input and passes
 * it on to</li>
 * <li>This executor, which performs a number of conversions in parallel and
 * passes these results and any resultant errors to</li>
 * <li>The accumulator, which creates an ordered list of the results.</li></ol></p>
 * <p>The threads in the executor queue their results in a thread-safe
 * queue, which should be O(1), minimizing wait time due to synchronization.
 * The accumulator then removes items from the queue and inserts them into a
 * sorted data structure, which is O(log n) on average and O(n) in the worst
 * case. If the user has told us she doesn't need sorted data, the
 * accumulator is not necessary, and thus is not started.</p>
 *
 * @param <T> The type of the object being created by the threads run
 * @author Andrew Rucker Jones
 * @since 4.0
 */
class IntolerantThreadPoolExecutor<T> extends ThreadPoolExecutor implements Spliterator<T> {

    /** A queue of the beans created. */
    protected final BlockingQueue<OrderedObject<T>> resultQueue = new LinkedBlockingQueue<>();

    /** A queue of exceptions thrown by threads during processing. */
    protected final BlockingQueue<OrderedObject<CsvException>> thrownExceptionsQueue = new LinkedBlockingQueue<>();

    /** A sorted, concurrent map for the beans created. */
    private ConcurrentNavigableMap<Long, T> resultantBeansMap = null;

    /**
     * A multi-valued map for any exceptions captured.
     * <p>The multi-valued part is important because the same line can throw more
     * than one exception.</p>
     * <p><em>All access to this variable must be synchronized.</em></p>
     * */
    private ListValuedMap<Long, CsvException> thrownExceptionsMap = null;

    /** A separate thread that accumulates and orders results. */
    protected AccumulateCsvResults<T> accumulateThread = null;

    /** A list of the ordinals of data records still to be expected by the accumulator. */
    protected final SortedSet<Long> expectedRecords = new ConcurrentSkipListSet<>();

    /**
     * Determines whether resulting data sets have to be in the same order as
     * the input.
     */
    private final boolean orderedResults;

    /** The locale for error messages. */
    protected final Locale errorLocale;

    /** The exception that caused this Executor to stop executing. */
    private Throwable terminalException;

    /**
     * Constructor for a thread pool executor that stops by itself as soon as
     * any thread throws an exception.
     * Threads never time out and the queue for inbound work is unbounded.
     * @param orderedResults Whether order should be preserved in the results
     * @param errorLocale The errorLocale to use for error messages.
     */
    IntolerantThreadPoolExecutor(boolean orderedResults, Locale errorLocale) {
        super(Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().availableProcessors(), Long.MAX_VALUE,
                TimeUnit.NANOSECONDS, new LinkedBlockingQueue<>());
        this.orderedResults = orderedResults;
        this.errorLocale = ObjectUtils.defaultIfNull(errorLocale, Locale.getDefault());
    }

    /**
     * Prepares this Executor to receive jobs.
     */
    public void prepare() {
        prestartAllCoreThreads();

        // The ordered maps and accumulator are only necessary if ordering is
        // stipulated. After this, the presence or absence of the accumulator is
        // used to indicate ordering or not so as to guard against the unlikely
        // problem that someone sets orderedResults right in the middle of
        // processing.
        if(orderedResults) {
            resultantBeansMap = new ConcurrentSkipListMap<>();
            thrownExceptionsMap = new ArrayListValuedHashMap<>();

            // Start the process for accumulating results and cleaning up
            accumulateThread = new AccumulateCsvResults<>(
                    resultQueue, thrownExceptionsQueue, expectedRecords,
                    resultantBeansMap, thrownExceptionsMap);
            accumulateThread.start();
        }
    }

    /**
     * Sends a signal to the Executor that it should shut down once all threads
     * have completed.
     *
     * @throws InterruptedException If the current thread is interrupted while
     * waiting. Shouldn't be thrown, since the Executor
     * waits indefinitely for all threads to end.
     * @throws RejectedExecutionException If an exception during processing
     * forced this Executor to shut down.
     */
    public void complete() throws InterruptedException {
        // Normal termination
        shutdown();
        awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS); // Wait indefinitely
        if(accumulateThread != null) {
            accumulateThread.setMustStop(true);
            accumulateThread.join();
        }

        // There's one more possibility: The very last bean caused a problem.
        if(terminalException != null) {
            // Trigger a catch in the calling method
            throw new RejectedExecutionException();
        }
    }

    /**
     * Returns exceptions captured during the conversion process if
     * the conversion process was set not to propagate these errors
     * up the call stack.
     * The call is nondestructive.
     *
     * @return All exceptions captured
     */
    public List<CsvException> getCapturedExceptions() {
        List<CsvException> returnList = null;
        if(thrownExceptionsMap == null) {
            returnList = thrownExceptionsQueue.stream()
                    .filter(Objects::nonNull)
                    .map(OrderedObject::getElement)
                    .collect(Collectors.toList());
        }
        else {
            returnList = new LinkedList<>();
            synchronized (thrownExceptionsMap) {
                final List<CsvException> finalReturnList = returnList;
                thrownExceptionsMap.keySet().stream()
                        .sorted()
                        .forEach(l -> finalReturnList.addAll(thrownExceptionsMap.get(l)));
            }
        }
        return returnList;
    }

    @Override
    public List<Runnable> shutdownNow() {
        if(accumulateThread != null) {
            accumulateThread.setMustStop(true);
            try {
                accumulateThread.join();
            } catch (InterruptedException e) {
                // Do nothing. Best faith effort.
            }
        }
        return super.shutdownNow();
    }

    /**
     * Shuts the Executor down if the thread ended in an exception.
     * @param r {@inheritDoc}
     * @param t {@inheritDoc} 
     */
    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        if(t != null) {
            if(t.getCause() != null) {
                // Normally, everything that gets to this point should be
                // wrapped in a RuntimeException to get past the lack of checked
                // exceptions in Runnable.run().
                terminalException = t.getCause();
            }
            else {
                terminalException = t;
            }
            shutdownNow();
        }
    }
    
    /**
     * If an unrecoverable exception was thrown during processing, it can be
     * retrieved here.
     * @return The exception that halted one of the threads, which caused the
     *   executor to shut itself down
     */
    public Throwable getTerminalException() {
        return terminalException;
    }

    /**
     * Checks whether exceptions are available that should halt processing.
     * This is the case with unrecoverable errors, such as parsing the input,
     * or if exceptions in conversion should be thrown by request of the user.
     */
    protected void checkExceptions() {
        if(terminalException != null) {
            if(terminalException instanceof CsvException) {
                CsvException csve = (CsvException) terminalException;
                throw new RuntimeException(String.format(ResourceBundle.getBundle(ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale).getString("parsing.error.linenumber"),
                        csve.getLineNumber(), String.join(",", ArrayUtils.nullToEmpty(csve.getLine()))), csve);
            }
            throw new RuntimeException(terminalException);
        }
    }

    private boolean isConversionComplete() {
        return isTerminated() && (accumulateThread == null || !accumulateThread.isAlive());
    }

    /**
     * Determines whether more conversion results can be expected.
     * Since {@link Spliterator}s have no way of indicating that they don't
     * have a result at the moment, but might in the future, we must ensure
     * that every call to {@link #tryAdvance(Consumer)} or {@link #trySplit()}
     * only returns {@code null} if the entire conversion apparatus has shut
     * down and all result queues are cleared. Thus, this method waits until
     * either that is true, or there is truly at least one result that can be
     * returned to users of the {@link Spliterator} interface.
     *
     * @return {@code false} if conversion is complete and no more results
     *   can ever be expected out of this {@link Spliterator}, {@code true}
     *   otherwise. If {@code true} is returned, it is guaranteed that at
     *   least one result is available immediately to the caller.
     */
    private boolean areMoreResultsAvailable() {
        // If an exception has been thrown that needs to be passed on,
        // throw it here.
        checkExceptions();

        // Check conditions for completion
        boolean elementFound = false;
        while(!elementFound && !isConversionComplete()) {
            if(accumulateThread == null) {
                if(resultQueue.isEmpty()) {
                    Thread.yield();
                }
                else {
                    elementFound = true;
                }
            }
            else {
                if(resultantBeansMap.isEmpty()) {
                    Thread.yield();
                }
                else {
                    elementFound = true;
                }
            }

            // If an exception has been thrown that needs to be passed on,
            // throw it here.
            checkExceptions();
        }

        return accumulateThread == null ? !resultQueue.isEmpty() : !resultantBeansMap.isEmpty();
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        T bean = null;

        if (areMoreResultsAvailable()) {
            // Since we are now guaranteed to have a result, we don't
            // really have to do all of the null checking below, but
            // better safe than sorry.
            if(accumulateThread == null) {
                OrderedObject<T> orderedObject = resultQueue.poll();
                if(orderedObject != null) {
                    bean = orderedObject.getElement();
                }
            }
            else {
                Map.Entry<Long, T> mapEntry = resultantBeansMap.pollFirstEntry();
                if(mapEntry != null) {
                    bean = mapEntry.getValue();
                }
            }
            if(bean != null) {
                action.accept(bean);
            }
        }

        return bean != null;
    }

    // WARNING! This code is untested because I have no way of telling the JDK
    // streaming code how to do its job.
    @Override
    public Spliterator<T> trySplit() {
        Spliterator<T> s = null;

        // Check if all threads are through
        if(areMoreResultsAvailable()) {
            if(isConversionComplete()) {
                // Return everything we have
                if(accumulateThread == null) {
                    s = resultQueue.stream().map(OrderedObject::getElement).spliterator();
                }
                else {
                    s = resultantBeansMap.values().spliterator();
                }
            }
            else {
                int size;
                ArrayList<T> c;
                if(accumulateThread == null) {
                    // May seem like an odd implementation, but we can't use
                    // resultQueue.drainTo() because bulk operations are not
                    // thread-safe. So, we have to poll each object individually.
                    // We don't want to use a LinkedList for the Spliterator
                    // because another split would presumably be inefficient. With
                    // an ArrayList, on the other hand, we have to make sure we
                    // avoid a costly resize operation.
                    size = resultQueue.size();
                    c = new ArrayList<>(size);
                    for(int i = 0; i < size; i++) {
                        // Result guaranteed to exist through areMoreResultsAvailable()
                        OrderedObject<T> orderedObject = resultQueue.poll();
                        if(orderedObject != null) {
                            c.add(orderedObject.getElement());
                        }

                    }
                }
                else {
                    size = resultantBeansMap.size();
                    c = new ArrayList<>(size);
                    for(int i = 0; i < size; i++) {
                        Map.Entry<Long, T> mapEntry = resultantBeansMap.pollFirstEntry();
                        if(mapEntry != null) {
                            c.add(mapEntry.getValue());
                        }
                    }
                }
                s = c.spliterator();
            }
        }

        return s;
    }

    // WARNING! This code is untested because I have no way of telling the JDK
    // streaming code how to do its job.
    @Override
    public long estimateSize() {
        return accumulateThread == null ? resultQueue.size() : resultantBeansMap.size();
    }

    @Override
    public int characteristics() {
        int characteristics = Spliterator.CONCURRENT | Spliterator.NONNULL;
        if(accumulateThread != null) {
            characteristics |= Spliterator.ORDERED;
        }
        return characteristics;
    }
}
