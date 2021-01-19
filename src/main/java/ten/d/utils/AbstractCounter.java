package ten.d.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicLong;


/**
 * This is an abstract class for TpsCounter, it gives ability to create other types of counters, which are not presented
 * in the current example since they are project-specific and could confuse.
 *
 * Main method here is incrementCounter();
 * */
public abstract class AbstractCounter <T extends AbstractCounter.TpsHolder> {
    private static final Logger log = LoggerFactory.getLogger(AbstractCounter.class);

    protected final LinkedList<T> TPS_LIST = new LinkedList<>();

    private final long startTime = System.currentTimeMillis();

    protected final int PERIOD;
    protected final int TPS_LIST_THRESHOLD;

    protected AbstractCounter(int tpsPeriod, int tpsListThreshold) {
        this.TPS_LIST_THRESHOLD = tpsListThreshold;
        this.PERIOD = tpsPeriod;
    }


    public final String incrementCounter() {
        try {
            long curntTime = System.currentTimeMillis();
            int periodsCount = (int) (curntTime - startTime) / PERIOD;

            T tpsHolder = takeTpsHolder(curntTime, periodsCount);
            boolean theVeryFirstTransaction = tpsHolder.counter.getAndIncrement() == 0;

            return theVeryFirstTransaction ? printHolderDataIfNecessary() : "";
        } catch (RuntimeException e) {
            log.info("Exception", e);
            return "";
        }
    }


    private String printHolderDataIfNecessary() {
        if (TPS_LIST.size() < TPS_LIST_THRESHOLD) {
            return "";
        }

        T lastDefinedTps;
        synchronized (this) {
            lastDefinedTps = TPS_LIST.removeFirst();
        }
        T nextOne = TPS_LIST.getFirst();

        if (TPS_LIST.size() > TPS_LIST_THRESHOLD) {
            log.warn("Attention, smth strange TPS_LIST.size(): {}, could lead to memory leak! To check", TPS_LIST.size());
        }


        return printHolders(lastDefinedTps, nextOne);
    }


    protected abstract String printHolders(T removedHolder, T nextOne);
    protected abstract T createTpsHolder(long curntTime, int periodsCount);


    private T takeTpsHolder(long curntTime, int periodsCount) {
        T lastHolder;
        if (TPS_LIST.size() == 0) {
            lastHolder = createVeryFirst(curntTime, periodsCount);
        } else {
            lastHolder = TPS_LIST.getLast();
        }

        if (lastHolder.periodsCount >= periodsCount) {
            return lastHolder;
        } else {
            return createNewPeriod(curntTime, periodsCount);
        }
    }


    private synchronized T createNewPeriod(long curntTime, int periodsCount) {
        T holder = TPS_LIST.getLast();
        if (holder.periodsCount >= periodsCount) {
            return holder;
        } else {
            return createHolder(curntTime, periodsCount);
        }
    }

    private synchronized T createVeryFirst(long curntTime, int periodsCount) {
        if (TPS_LIST.size() == 0) {
            return createHolder(curntTime, periodsCount);
        } else {
            return TPS_LIST.getLast();
        }
    }

    private T createHolder(long curntTime, int periodsCount) {
        T holder = createTpsHolder(curntTime, periodsCount);
        TPS_LIST.add(holder);
        return holder;
    }


    public class TpsHolder {
        public final long time;
        public final int periodsCount;
        public final AtomicLong counter = new AtomicLong(0);

        public TpsHolder(long time, int count) {
            this.time = time;
            this.periodsCount = count;
        }
    }
}
