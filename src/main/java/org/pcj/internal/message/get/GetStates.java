package org.pcj.internal.message.get;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.pcj.PcjFuture;
import org.pcj.PcjRuntimeException;

public class GetStates {
    private final AtomicInteger counter;
    private final ConcurrentMap<Integer, State<?>> stateMap;

    public GetStates() {
        counter = new AtomicInteger(0);
        stateMap = new ConcurrentHashMap<>();
    }

    public <T> State<T> create() {
        int requestNum = counter.incrementAndGet();

        GetFuture<T> future = new GetFuture<>();
        State<T> state = new State<>(requestNum, future);

        stateMap.put(requestNum, state);

        return state;
    }

    public State<?> remove(int requestNum) {
        return stateMap.remove(requestNum);
    }

    public class State<T> {

        private final int requestNum;
        private final GetFuture<T> future;

        public State(int requestNum, GetFuture<T> future) {
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
                PcjRuntimeException ex = new PcjRuntimeException("Getting value failed");
                ex.addSuppressed(exception);
                future.signalException(ex);
            }
        }
    }
}
