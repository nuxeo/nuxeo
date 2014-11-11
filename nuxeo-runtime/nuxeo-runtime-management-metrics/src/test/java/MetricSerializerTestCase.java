import java.io.IOException;

import junit.framework.TestCase;

import org.javasimon.Sample;
import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.nuxeo.runtime.management.metrics.MetricSerializer;

public class MetricSerializerTestCase extends TestCase {

    final MetricSerializer srv = new MetricSerializer();

    static class Context {
        public String getInfo() {
            return "info";
        }
    }

    Sample newSample() {
        try {
            Stopwatch stopwatch = SimonManager.getStopwatch("test");
            stopwatch.setAttribute("ctx", new Context());
            Split split = stopwatch.start();
            split.stop();
            return stopwatch.sample();
        } finally {
            SimonManager.destroySimon("test");
        }
    }

    public void testService() throws IOException {
        srv.resetOutput();
        srv.getOutputFile().deleteOnExit();
        srv.toStream(newSample());
        srv.flushOuput();
        assertTrue(srv.getOutputFile().length() > 0);
    }

}
