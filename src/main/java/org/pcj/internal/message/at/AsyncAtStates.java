package org.pcj.internal.message.at;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.pcj.PcjFuture;
import org.pcj.PcjRuntimeException;

public class AsyncAtStates {
    private final AtomicInteger counter;
    private final ConcurrentMap<Integer, State<?>> stateMap;

    public AsyncAtStates() {
        counter = new AtomicInteger(0);
        stateMap = new ConcurrentHashMap<>();
    }

    public <T> State<T> create() {
        int requestNum = counter.incrementAndGet();

        AsyncAtFuture<T> future = new AsyncAtFuture<>();
        State<T> state = new State<>(requestNum, future);

        stateMap.put(requestNum, state);

        return state;
    }

    public State<?> remove(int requestNum) {
        return stateMap.remove(requestNum);
    }

    public class State<T> {

        private final int requestNum;
        private final AsyncAtFuture<T> future;

        public State(int requestNum, AsyncAtFuture<T> future) {
            this.requestNum = requestNum;

            this.future = future;
        }

        public int getRequestNum() {
            return requestNum;
        }

        public PcjFuture<T> getFuture() {
            return future;
        }

        public void signal(Object variableValue, Exception exception) {
            if (exception == null) {
                future.signalDone(variableValue);
            } else {
                PcjRuntimeException ex = new PcjRuntimeException("Exception while asynchronous execution.");
                ex.addSuppressed(exception);
                future.signalException(ex);
            }
        }
    }
}
