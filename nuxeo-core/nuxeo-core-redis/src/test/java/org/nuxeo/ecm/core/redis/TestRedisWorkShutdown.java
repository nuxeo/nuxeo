package org.nuxeo.ecm.core.redis;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkQueueMetrics;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import redis.clients.jedis.Jedis;

@Features({ RedisFeature.class, CoreFeature.class })
@RunWith(FeaturesRunner.class)
public class TestRedisWorkShutdown {

    static Log log = LogFactory.getLog(TestRedisWorkShutdown.class);

    static CountDownLatch canShutdown = new CountDownLatch(2);

    static CountDownLatch canProceed = new CountDownLatch(1);

    public static class MyWork extends AbstractWork {

        private static final long serialVersionUID = 1L;


        MyWork(String id) {
            super(id);
            setProgress(new Progress(0,2));
        }

        @Override
        public String getTitle() {
            return "waiting work";
        }

        Progress nextProgress() {
            Progress progress = getProgress();
            progress = new Progress(progress.getCurrent()+1, progress.getTotal());
            setProgress(progress);
            return progress;
        }
        @Override
        public void work() {
            Progress progress = nextProgress();
            if (progress.getCurrent() < progress.getTotal()) {
                try {
                    log.debug(id + " waiting for shutdown");
                    canShutdown.countDown();
                    canProceed.await(1, TimeUnit.MINUTES);
                    Assert.assertTrue(isSuspending());
                    suspended();
                } catch (InterruptedException cause) {
                    Thread.currentThread()
                            .interrupt();
                    throw new RuntimeException(cause);
                }
            } else {
                ;
            }
        }

        @Override
        public String toString() {
            return id;
        }
    }

    @Inject
    WorkManager works;

    void assertMetrics(long scheduled, long running, long completed, long cancelled) {
        assertEquals(new WorkQueueMetrics("default", scheduled, running, completed, cancelled), works.getMetrics("default"));
    }

    @Test
    public void worksArePersisted() throws InterruptedException {
        assertMetrics(0, 0, 0, 0);
        try {
            // given two running works
            works.schedule(new MyWork("first"));
            works.schedule(new MyWork("second"));
            canShutdown.await(10, TimeUnit.SECONDS);
            assertMetrics(0, 2, 0, 0);
            // when I shutdown
            Framework.getRuntime().standby(Instant.now().plus(Duration.ofSeconds(10)));
        } finally {
            // then works are suspending
            canProceed.countDown();
        }
        // then works are re-scheduled
        try {
            List<Work> scheduled = new ScheduledRetriever().listScheduled();
            Assert.assertThat(scheduled.size(), Matchers.is(2));
            canProceed = new CountDownLatch(1);
        } finally {
            // when I reboot
            Framework.getRuntime().resume();
        }
        Assert.assertTrue(works.awaitCompletion(10, TimeUnit.SECONDS));
        // works are completed
        assertMetrics(0, 0, 2, 2);
    }

    class ScheduledRetriever {
        String namespace = Framework.getService(RedisAdmin.class)
                .namespace("work");

        byte[] keyBytes(String value) {
            try {
                return namespace.concat(value)
                        .getBytes("UTF-8");
            } catch (UnsupportedEncodingException cause) {
                throw new UnsupportedOperationException("Cannot encode " + value, cause);
            }
        }

        byte[] queueBytes() {
            return keyBytes("sched:default");
        }

        byte[] dataKey() {
            return keyBytes("data");
        }

        List<Work> listScheduled() {
            RedisPoolDescriptor config = Framework.getService(RedisAdmin.class).getConfig();
            return config.newExecutor()
                    .execute(new RedisCallable<List<Work>>() {
                        @Override
                        public List<Work> call(Jedis jedis) {
                            Set<byte[]> keys = jedis.smembers(queueBytes());
                            List<Work> list = new ArrayList<Work>(keys.size());
                            for (byte[] workIdBytes : keys) {
                                // get data
                                byte[] workBytes = jedis.hget(dataKey(), workIdBytes);
                                Work work = deserializeWork(workBytes);
                                list.add(work);
                            }
                            return list;
                        }
                    });
        }

        Work deserializeWork(byte[] bytes) {
            if (bytes == null) {
                return null;
            }
            InputStream bain = new ByteArrayInputStream(bytes);
            try (ObjectInputStream in = new ObjectInputStream(bain)) {
                return (Work) in.readObject();
            } catch (RuntimeException cause) {
                throw cause;
            } catch (IOException | ClassNotFoundException cause) {
                throw new RuntimeException("Cannot deserialize work", cause);
            }
        }
    }
}
