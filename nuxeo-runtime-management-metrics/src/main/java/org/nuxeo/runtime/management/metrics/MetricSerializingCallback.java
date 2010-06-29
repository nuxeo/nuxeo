package org.nuxeo.runtime.management.metrics;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.javasimon.CallbackSkeleton;
import org.javasimon.Counter;
import org.javasimon.Sample;
import org.javasimon.Split;

public class MetricSerializingCallback extends CallbackSkeleton {

    protected static Log log = LogFactory.getLog(MetricSerializingCallback.class);

    protected MetricSerializer serializer;

    public MetricSerializingCallback(MetricSerializer serializer) {
        this.serializer = serializer;
    }


    @Override
    public void stopwatchStop(Split split) {
       toStream(split.getStopwatch().sample());
    }

    @Override
    public void counterSet(Counter counter, long val) {
      toStream(counter.sample());
    }

    protected void toStream(Sample sample) {
        try {
            serializer.toStream(sample);
        } catch (IOException e) {
            log.info("not streamed " +sample, e);
        }
    }

}
