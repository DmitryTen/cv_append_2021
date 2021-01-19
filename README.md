This repo is used to show my code skills during job-search in 2021.


I've decided to upload a util class which i broadly use in my projects.
The main purpose of this util - load estimation in time.

Sometimes it's not that clear how many threads pass through specific part of code per minute/hour/day, while this data is essential.

This util solves this small issue.

Usecase:

    public class SmscToMgwServlet {
        private static final Logger log = LoggerFactory.getLogger(SmscToMgwServlet.class);
        private final TpsCounter TPS_COUNTER_1 = TpsCounter.createTpsCounter(TpsCounter.Period.MINUTE);
         
        private void processMessage(long id, byte[] data){
             log.info("Receive MT message, id {}, {}", msgId, TPS_COUNTER_1.defineTpsPerPeriod());
             //business flow
             ...
        }
        ...
    }


To see result data, the only you need is to grep application logs, for example as below:

grep 'Receive MT message' platform.log | grep tps

    2021-01-19 02:55:08.288 INFO [qtp575335780-119100] (SmscToMgwServlet.java:153) - Receive MT message, id 1616783625, [total trn: 12, tps: 0.1875 trn/s (from 02:53:13.953 to 02:54:18.534)]
    2021-01-19 02:56:08.833 INFO [qtp575335780-118924] (SmscToMgwServlet.java:153) - Receive MT message, id 1616783646, [total trn: 10, tps: 0.2041 trn/s (from 02:54:18.534 to 02:55:08.288)]
    2021-01-19 02:57:10.114 INFO [qtp575335780-119100] (SmscToMgwServlet.java:153) - Receive MT message, id 1616783667, [total trn: 18, tps: 0.3000 trn/s (from 02:55:08.288 to 02:56:08.833)]
    2021-01-19 02:58:10.072 INFO [qtp575335780-119215] (SmscToMgwServlet.java:153) - Receive MT message, id 1616783683, [total trn: 22, tps: 0.3607 trn/s (from 02:56:08.833 to 02:57:10.114)]
    ...
    2021-01-19 11:38:06.478 INFO [qtp575335780-131673] (SmscToMgwServlet.java:153) - Receive MT message, id 1616971328, [total trn: 440, tps: 7.4576 trn/s (from 11:36:06.478 to 11:37:06.396)]
    2021-01-19 11:39:06.492 INFO [qtp575335780-131661] (SmscToMgwServlet.java:153) - Receive MT message, id 1616972123, [total trn: 459, tps: 7.6500 trn/s (from 11:37:06.396 to 11:38:06.478)]
    2021-01-19 11:40:06.439 INFO [qtp575335780-125276] (SmscToMgwServlet.java:153) - Receive MT message, id 1616972760, [total trn: 426, tps: 7.1000 trn/s (from 11:38:06.478 to 11:39:06.492)]
    2021-01-19 11:41:06.433 INFO [qtp575335780-128019] (SmscToMgwServlet.java:153) - Receive MT message, id 1616973592, [total trn: 475, tps: 8.0508 trn/s (from 11:39:06.492 to 11:40:06.439)]
    2021-01-19 11:42:06.410 INFO [qtp575335780-131678] (SmscToMgwServlet.java:153) - Receive MT message, id 1616974408, [total trn: 460, tps: 7.7966 trn/s (from 11:40:06.439 to 11:41:06.433)]

the tps could be easily monitored: [total trn: 12, tps: 0.1875 trn/s (from 02:53:13.953 to 02:54:18.534)]

Regards, Dmitriy

