package ten.d.utils;

import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This Utility class for counting TPS in multithreaded applications.
 * It gives ability to keep finger on the pulse of application load.
 *
 * To view a use example see ten.d.TpsCounterUseExmpl.
 *
 *
 * Here is below a real example.
 *
 *     private final TpsCounter ussdMtTps = TpsCounter.createTpsCounter(TpsCounter.Period.MINUTE);
 *
 *     ...
 *
 *     log.info("Receive MT message, id {}, {}", msgId, ussdMtTps.defineTpsPerPeriod());
 *
 * To see the data, the only you need is to grep application logs, for example as below:
 *
 * grep 'Receive MT message' platform.log | grep tps
 *
 * 2021-01-19 02:55:08.288 INFO [qtp575335780-119100] (SmscToMgwServlet.java:153) - Receive MT message, id 1616783625, [total trn: 12, tps: 0.1875 trn/s (from 02:53:13.953 to 02:54:18.534)]
 * 2021-01-19 02:56:08.833 INFO [qtp575335780-118924] (SmscToMgwServlet.java:153) - Receive MT message, id 1616783646, [total trn: 10, tps: 0.2041 trn/s (from 02:54:18.534 to 02:55:08.288)]
 * 2021-01-19 02:57:10.114 INFO [qtp575335780-119100] (SmscToMgwServlet.java:153) - Receive MT message, id 1616783667, [total trn: 18, tps: 0.3000 trn/s (from 02:55:08.288 to 02:56:08.833)]
 * 2021-01-19 02:58:10.072 INFO [qtp575335780-119215] (SmscToMgwServlet.java:153) - Receive MT message, id 1616783683, [total trn: 22, tps: 0.3607 trn/s (from 02:56:08.833 to 02:57:10.114)]
 * ...
 * 2021-01-19 11:38:06.478 INFO [qtp575335780-131673] (SmscToMgwServlet.java:153) - Receive MT message, id 1616971328, [total trn: 440, tps: 7.4576 trn/s (from 11:36:06.478 to 11:37:06.396)]
 * 2021-01-19 11:39:06.492 INFO [qtp575335780-131661] (SmscToMgwServlet.java:153) - Receive MT message, id 1616972123, [total trn: 459, tps: 7.6500 trn/s (from 11:37:06.396 to 11:38:06.478)]
 * 2021-01-19 11:40:06.439 INFO [qtp575335780-125276] (SmscToMgwServlet.java:153) - Receive MT message, id 1616972760, [total trn: 426, tps: 7.1000 trn/s (from 11:38:06.478 to 11:39:06.492)]
 * 2021-01-19 11:41:06.433 INFO [qtp575335780-128019] (SmscToMgwServlet.java:153) - Receive MT message, id 1616973592, [total trn: 475, tps: 8.0508 trn/s (from 11:39:06.492 to 11:40:06.439)]
 * 2021-01-19 11:42:06.410 INFO [qtp575335780-131678] (SmscToMgwServlet.java:153) - Receive MT message, id 1616974408, [total trn: 460, tps: 7.7966 trn/s (from 11:40:06.439 to 11:41:06.433)]
 *
 * invoking the above grep we able to view the load every minute.
 *
 * */
public class TpsCounter extends AbstractCounter<TpsCounter.TpsCounterHolder>{
    private static final Logger log = LoggerFactory.getLogger(TpsCounter.class);

    /**
     * ACCURACY_100
     * element0 - holder with tps data, already counted.
     * element1 - holder with tps data, counted on 99% (in high multi thread load, some threads are delaying (less then
     * 0.1% on my test, but in practice, it could differ, but i'm sure it'll be lower than 1%))
     * element2 - holder on active count, when it appears element0 is removing and to placing to 'lastDefinedTps'
     *
     * ACCURACY_99
     * element1 - holder with tps data, counted on 99% (in high multi thread load, some threads are delaying (less then
     * 0.1% on my test, but in practice, it could differ, but i'm sure it'll be lower than 1%))
     * element2 - holder on active count, when it appears element0 is removing and to placing to 'lastDefinedTps'
     *
     * */
    public enum Mode {
        /**
         * 100% accuracy, but tps is lagged on one period (second or minute or hour)
         * */
        ACCURACY_100(3),

        /**
         * Could have lower accuracy.
         * During the test it shown 99.999% accuracy (more load more accuracy), but it could differ.
         *
         * */
        ACCURACY_99(2);

        int listThreshold;

        Mode(int listThreshold) {
            this.listThreshold = listThreshold;
        }
    }


    private TpsCounter(int tpsPeriod, int tpsListThreshold) {
        super(tpsPeriod, tpsListThreshold);
    }




    private PublicTpsInfo publicTpsInfo;


    /**
     * The main method to be used inside log.
     * */
    public String defineTpsPerPeriod() {
        return incrementCounter();
    }

    public PublicTpsInfo getLastCounter(){
        if (publicTpsInfo == null) {
            return null;
        } else {
            return publicTpsInfo;
        }
    }


    @Override
    protected String printHolders(TpsCounterHolder removedHolder, TpsCounterHolder nextOne) {
        publicTpsInfo = new PublicTpsInfo(
                removedHolder.time,
                nextOne.time,
                removedHolder.counter.get(),
                PERIOD,
                removedHolder.periodsCount,
                nextOne.periodsCount);
        return publicTpsInfo.printTps();
    }

    @Override
    protected TpsCounterHolder createTpsHolder(long curntTime, int periodsCount) {
        return new TpsCounterHolder(curntTime, periodsCount);
    }

    protected class TpsCounterHolder extends AbstractCounter.TpsHolder{
        public TpsCounterHolder(long time, int count) {
            super(time, count);
        }
    }

    private static final FastDateFormat fdf = FastDateFormat.getInstance("HH:mm:ss.SSS");
    public class PublicTpsInfo {
        final long fromTime;
        final long toTime;
        final long totalCount;
        final int period;
        final int countId;
        final int nextCountId;

        public PublicTpsInfo(long fromTime, long toTime, long totalCount, int period, int countId, int nextCountId) {
            this.fromTime = fromTime;
            this.toTime = toTime;
            this.totalCount = totalCount;
            this.period = period;
            this.countId = countId;
            this.nextCountId = nextCountId;
        }

        public int getNextCountId() {
            return nextCountId;
        }

        public long getFromTime() {
            return fromTime;
        }

        public long getToTime() {
            return toTime;
        }

        public long getTotalCount() {
            return totalCount;
        }

        public int getPeriod() {
            return period;
        }

        public int getCountId() {
            return countId;
        }

        @Override
        public String toString() {
            return "PublicTpsInfo{" +
                    "fromTime=" + fromTime +
                    ", toTime=" + toTime +
                    ", totalCount=" + totalCount +
                    ", period=" + period +
                    ", countId=" + countId +
                    ", nextCountId=" + nextCountId +
                    '}';
        }

        public String printTps(){
            return String.format("[total trn: %d, tps: %.4f trn/s (from %s to %s)]",
                    totalCount,
                    totalCount / (float) ((toTime - fromTime)/1000),
                    fdf.format(new Date(fromTime)),
                    fdf.format(new Date(toTime))
            );
        }
    }

    public enum Period {
        SECOND(1000),
        MINUTE(60_000),
        HOUR(3_600_000),
        DAY(24 * 3_600_000),
        WEEK(7 * 24 * 3_600_000),
        ;
        int millis;

        Period(int millis) {
            this.millis = millis;
        }
    }


    public static TpsCounter createTpsCounter(Period period) {
        switch (period) {
            case SECOND:
            case MINUTE: return createTpsCounter(period, Mode.ACCURACY_100);
            case HOUR:
            case DAY:
            case WEEK: return createTpsCounter(period, Mode.ACCURACY_99);
        }
        throw new IllegalStateException();
    }

    public static TpsCounter createTpsCounter(Period period, Mode mode) {
        return new TpsCounter(period.millis, mode.listThreshold);
    }

    public static TpsCounter createTpsCounter_custom(int period, Mode mode) {

        return new TpsCounter(period, mode.listThreshold);
    }

    public static void main(String[] args) throws Exception {
        Pattern PATTERN = Pattern.compile("\\[total trn: ([0-9]+)(.*)");
        long time = System.currentTimeMillis();

        TpsCounter counter = TpsCounter.createTpsCounter(Period.SECOND);

        int THREAD_AMT = 20;
        int ITERATION_CNT = 20_000_000;

        System.out.println(String.format("start. Total iterations in the end have to be equal to: %d", THREAD_AMT * ITERATION_CNT));

        List<Integer> COUNT_LIST = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();
        for(int i = 0; i < THREAD_AMT; i++) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (int k = 0; k < ITERATION_CNT; k++) {
                            String tps = counter.defineTpsPerPeriod();

                            if (!tps.equals("")) {
                                COUNT_LIST.add(printDataToConsole_getTps(tps, counter.getLastCounter(), PATTERN));
                            }
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }, String.format("%02d", i));
            threads.add(thread);
            thread.start();
        }

        for(Thread thread : threads) {
            thread.join();
        }
        Thread.sleep(Period.SECOND.millis);
        COUNT_LIST.add(printDataToConsole_getTps(counter.defineTpsPerPeriod(), counter.getLastCounter(), PATTERN));
        Thread.sleep(Period.SECOND.millis);
        COUNT_LIST.add(printDataToConsole_getTps(counter.defineTpsPerPeriod(), counter.getLastCounter(), PATTERN));
        System.out.println(String.format("result: %d, time %d milliseconds",
                COUNT_LIST.stream().reduce(0, (a, b) -> a + b).longValue(),
                System.currentTimeMillis() - time));
    }
    private static Integer printDataToConsole_getTps(String txt, PublicTpsInfo info, Pattern PATTERN) {
        System.out.println(String.format("log '%s'",txt));
        System.out.println(String.format("full data: %s", info.toString()));
        Matcher matcher = PATTERN.matcher(txt);
        matcher.find();
        return new Integer(matcher.group(1));
    }


}
