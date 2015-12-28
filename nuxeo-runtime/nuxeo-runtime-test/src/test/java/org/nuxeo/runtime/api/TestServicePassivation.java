/*******************************************************************************
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *******************************************************************************/
package org.nuxeo.runtime.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.api.ServicePassivator.Termination;
import org.nuxeo.runtime.api.ServicePassivator.Termination.Failure;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestServicePassivation {

    final ExecutorService executor = Executors.newCachedThreadPool();

    public static class Service extends DefaultComponent {

        boolean forwardedCall;

        public void awaitThenForward(CountDownLatch entered, CountDownLatch canProceed) throws InterruptedException {
            entered.countDown();
            if (canProceed.await(10, TimeUnit.SECONDS) == false) {
                throw new AssertionError("Cannot proceed");
            }
            forwardedCall = Framework.getService(Provider.class).forward();
        }

        public void enterQuiet() {
            assertThat(DefaultServiceProvider.getProvider()).isNull();
        }
    }

    public static class Provider extends DefaultComponent {

        final AtomicInteger counter = new AtomicInteger();

        public boolean forward() {
            LogFactory.getLog(Provider.class).debug("forwarded " + counter.addAndGet(1));
            return true;
        }

    }

    @Test
    @LocalDeploy({ "org.nuxeo.runtime:passivation-test-service.xml",
            "org.nuxeo.runtime:passivation-test-provider.xml" })
    public void lookupsAreBlocked() throws InterruptedException, ExecutionException {
        abstract class AbstractTask implements Runnable {

            Future<?> future;

            @SuppressWarnings("unchecked")
            <T> T submit() {
                future = executor.submit(this);
                return (T) this;
            }

            void await() {
                try {
                    future.get(10, TimeUnit.SECONDS);
                } catch (InterruptedException cause) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError("Interruped while synching with task", cause);
                } catch (ExecutionException | TimeoutException cause) {
                    throw new AssertionError("Cannot synch with task", cause);
                } finally {
                    future = null;
                }
            }

        }
        class AwaitThenForward extends AbstractTask {

            final CountDownLatch entered = new CountDownLatch(1);

            final CountDownLatch canProceed = new CountDownLatch(1);

            void unlock() {
                canProceed.countDown();
            }

            void unlockAndAwait() {
                unlock();
                await();
            }

            @Override
            public void run() {
                try {
                    Framework.getService(Service.class).awaitThenForward(entered, canProceed);
                } catch (InterruptedException cause) {
                    throw new AssertionError("Cannot proceed in service");
                }
            }

            @Override
            <T> T submit() {
                try {
                    return super.submit();
                } finally {
                    try {
                        entered.await(10, TimeUnit.SECONDS);
                    } catch (InterruptedException cause) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError("Timed out while waiting for service entered", cause);
                    }
                }
            }

        }

        class EnterQuiet extends AbstractTask {

            @Override
            public void run() {
                Framework.getService(Service.class).enterQuiet();
            }

        }

        AwaitThenForward canProceed = new AwaitThenForward().submit();
        AwaitThenForward wrongAccess = new AwaitThenForward().submit();
        EnterQuiet enterQuiet = new EnterQuiet();

        Termination termination = ServicePassivator
                .passivate()
                .peek(passivator -> canProceed.unlockAndAwait())
                .withQuietDelay(Duration.ofSeconds(1))
                .monitor()
                .peek(passivator -> enterQuiet.<EnterQuiet>submit())
                .withTimeout(Duration.ofSeconds(5))
                .await()
                .proceed(() -> wrongAccess.unlockAndAwait())
                .peek(self -> enterQuiet.await());
        assertThat(termination).isInstanceOf(Failure.class);
    }
}
