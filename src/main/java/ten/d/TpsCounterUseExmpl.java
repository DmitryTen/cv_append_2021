package ten.d;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ten.d.utils.TpsCounter;



public class TpsCounterUseExmpl {
    private static final Logger log = LoggerFactory.getLogger(TpsCounter.class);

    TpsCounter MINUTE_COUNTER = TpsCounter.createTpsCounter(TpsCounter.Period.MINUTE);

    public void processData(long id, byte[] data) {
        log.info("started to process data, id {}. {}", id, MINUTE_COUNTER.defineTpsPerPeriod());

        //business logic
    }

}
