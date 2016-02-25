/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nuxeo.runtime.api;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.ServicePassivator.Passivator.Accounting;
import org.nuxeo.runtime.api.ServicePassivator.Passivator.Accounting.InScopeOfContext;
import org.nuxeo.runtime.api.ServicePassivator.Termination.Failure;
import org.nuxeo.runtime.api.ServicePassivator.Termination.Success;
import org.nuxeo.runtime.model.ComponentManager;

/**
 * Blocks service accesses in order to run an operation which alter the runtime. That gives a way to prevent service
 * consumers to enter during the shutdown or the reload operation.
 * <p>
 * The invoke chain is split in the following steps
 * <dl>
 * <dt>passivate</dt>
 * <dd>intercept service lookup</dd>
 * <dt>monitor</dt>
 * <dd>monitor service pass-through accesses</dd>
 * <dt>await
 * <dt>
 * <dd>wait for the runtime being quiet before proceeding</dd>
 * <dt>proceed</dt>
 * <dd>proceed with the operation and handle termination hook</dd>
 * </dl>
 *
 * <pre>
 * ServicePassivator
 *         .passivate()
 *         .withQuietDelay(ChronoUnit.SECONDS.getDuration().multipliedBy(20))
 *         .monitor()
 *         .withTimeout(ChronoUnit.MINUTES.getDuration().multipliedBy(2))
 *         .await()
 *         .proceed(() -> System.out.println("do something"))
 *         .onFailure(failure -> System.out.println("failed " + failure))
 *         .onSuccess(() -> System.out.println("succeed"));*
 * </pre>
 * </p>
 *
 * @since 8.1
 */
public class ServicePassivator {

    public static Passivator passivate() {
        return new Passivator();
    }

    public static Termination proceed(Duration quiet, Duration timeout, boolean enforce, Runnable runnable) {
        return passivate()
                .withQuietDelay(quiet)
                .monitor()
                .withTimeout(timeout)
                .withEnforceMode(enforce)
                .await()
                .proceed(runnable);
    }

    public static <X extends Exception> void proceed(Duration quiet, Duration timeout,
            boolean enforce, RunnableCheckException<Exception> runnable, Class<X> oftype) throws X {
        class CheckExceptionHolder extends RuntimeException {

            private static final long serialVersionUID = 1L;

            CheckExceptionHolder(Throwable cause) {
                super(cause);
            }

            void rethrow(Class<X> oftype) throws X {
                if (getCause() instanceof InterruptedException) {
                    Thread.currentThread()
                            .interrupt();
                }
                throw oftype.cast(getCause());
            }
        }
        try {
            ServicePassivator.passivate()
                    .withQuietDelay(quiet)
                    .monitor()
                    .withTimeout(timeout)
                    .withEnforceMode(enforce)
                    .await()
                    .proceed(() -> {
                        try {
                            runnable.run();
                        } catch (Exception cause) {
                            throw new CheckExceptionHolder(cause);
                        }
                    });
        } catch (CheckExceptionHolder cause) {
            cause.rethrow(oftype);
        }

    }

    public interface RunnableCheckException<X extends Exception> {
        void run() throws X;
    }

    /**
     * Intercepts service lookups for implementing the quiet logic.
     */
    public static class Passivator {

        final Log log = LogFactory.getLog(ServicePassivator.class);

        Passivator() {
            run();
        }

        final CountDownLatch achieved = new CountDownLatch(1);

        final Accounting accounting = new Accounting();

        Optional<ServiceProvider> installed = Optional.empty();

        void run() {
            installed = Optional.ofNullable(DefaultServiceProvider.getProvider());
            ServiceProvider passthrough = installed.map(
                    (Function<ServiceProvider, ServiceProvider>) DelegateProvider::new)
                    .orElseGet(
                            (Supplier<ServiceProvider>) RuntimeProvider::new);
            ServiceProvider waitfor = new WaitForProvider(achieved, passthrough);
            PassivateProvider passivator = new PassivateProvider(Thread.currentThread(), accounting, waitfor,
                    passthrough);
            DefaultServiceProvider.setProvider(passivator);
            log.debug("installed passivator", log.isTraceEnabled() ? new Throwable("stack trace") : null);
        }

        void commit() {
            try {
                DefaultServiceProvider.setProvider(installed.orElse(null));
            } finally {
                achieved.countDown();
                log.debug("uninstalled passivator");
            }
        }

        TemporalAmount quietDelay = Duration.ofSeconds(5);

        public Passivator withQuietDelay(TemporalAmount delay) {
            quietDelay = delay;
            return this;
        }

        public Passivator peek(Consumer<Passivator> consumer) {
            consumer.accept(this);
            return this;
        }

        public Monitor monitor() {
            return new Monitor(this, quietDelay);
        }

        /**
         * Snapshots service lookups and states about service scoping. *
         */
        public class Accounting {

            /**
             * Takes a snapshot of the lookup.
             *
             * @param typeof
             * @return
             */
            Optional<InScopeOfContext> take(Class<?> serviceof) {
                Class<?>[] callstack = dumper.dump();
                Optional<InScopeOfContext> snapshot = inscopeof(callstack)
                        .map(inscopeof -> new InScopeOfContext(inscopeof, Thread.currentThread(), callstack));
                snapshot.ifPresent(this::register);
                return snapshot;
            }

            void register(InScopeOfContext context) {
                last = Optional.of(context);
            }

            volatile Optional<InScopeOfContext> last = Optional.empty();

            public Optional<InScopeOfContext> get() {
                return last;
            }

            Optional<InScopeOfContext> reset() {
                try {
                    return last;
                } finally {
                    last = Optional.empty();
                }
            }

            Optional<Class<?>> inscopeof(Class<?>[] callstack) {
                final ComponentManager cm = Framework.getRuntime()
                        .getComponentManager();
                if (cm != null) {

                    for (Class<?> typeof : callstack) {
                        if (cm.getComponentProvidingService(typeof) != null) {
                            return Optional.of(typeof);
                        }
                    }
                }
                return Optional.empty();
            }

            final CallstackDumper dumper = new CallstackDumper();

            /**
             * Scoped service call context.
             */
            public class InScopeOfContext {

                InScopeOfContext(Class<?> serviceof, Thread thread, Class<?>[] callstack) {
                    this.serviceof = serviceof;
                    this.thread = thread;
                    this.callstack = callstack;
                }

                final Class<?> serviceof;

                final Thread thread;

                final Class<?>[] callstack;

                @Override
                public String toString() {
                    StringBuilder builder = new StringBuilder().append("on ")
                            .append(thread)
                            .append(" in scope of ")
                            .append(serviceof)
                            .append(System.lineSeparator());
                    for (Class<?> typeof : callstack) {
                        builder = builder.append("  ")
                                .append(typeof)
                                .append(System.lineSeparator());
                    }
                    return builder.toString();
                }
            }

            /**
             * Dumps caller stack and states for a service scope
             */
            class CallstackDumper extends SecurityManager {

                Class<?>[] dump() {
                    return super.getClassContext();
                }

            }

        }

    }

    /**
     * Monitors service lookups for stating about quiet status.
     */
    public static class Monitor {

        Monitor(Passivator passivator, TemporalAmount quietDelay) {
            this.passivator = passivator;
            this.quietDelay = quietDelay;
            run();
        }

        final Passivator passivator;

        final CountDownLatch passivated = new CountDownLatch(1);

        final TemporalAmount quietDelay;

        final Timer timer = new Timer(ServicePassivator.class.getSimpleName()
                .toLowerCase());

        final TimerTask scheduledTask = new TimerTask() {

            @Override
            public void run() {
                Optional<InScopeOfContext> observed = passivator.accounting.reset();
                if (observed.isPresent()) {
                    return;
                }
                cancel();
                passivated.countDown();
            }
        };

        void run() {
            long delay = TimeUnit.MILLISECONDS.convert(quietDelay.get(ChronoUnit.SECONDS), TimeUnit.SECONDS);
            if (delay <= 0) {
                passivated.countDown();
                return;
            }
            timer.scheduleAtFixedRate(
                    scheduledTask,
                    delay,
                    delay);
            passivator.log.debug("monitoring accesses");
        }

        /**
         * Cancels service lookups monitoring.
         */
        void cancel() {
            try {
                timer.cancel();
            } finally {
                passivator.commit();
            }
        }

        TemporalAmount timeout = Duration.ofSeconds(30);

        boolean enforce = true;

        public Monitor withTimeout(TemporalAmount value) {
            timeout = value;
            return this;
        }

        public Monitor withEnforceMode(boolean value) {
            enforce = value;
            return this;
        }

        public Monitor peek(Consumer<Monitor> consumer) {
            consumer.accept(this);
            return this;
        }

        /**
         * Installs the timer task which monitor the service lookups. Once there will be no more lookups in the
         * scheduled period, notifies the runner for proceeding.
         *
         * @param next
         * @return
         */
        public Waiter await() {
            return new Waiter(this, timeout, enforce);
        }

    }

    /**
     * Terminates the chain by running the operation in a passivated context.
     */
    public static class Waiter {

        Waiter(Monitor monitor, TemporalAmount timeout, boolean enforce) {
            this.monitor = monitor;
            this.timeout = timeout;
            this.enforce = enforce;
        }

        final Monitor monitor;

        final TemporalAmount timeout;

        final boolean enforce;

        public Waiter peek(Consumer<Waiter> consumer) {
            consumer.accept(this);
            return this;
        }

        /**
         * Terminates the chain by invoking the operation
         * <ul>
         * <li>waits for the runtime being passivated,</li>
         * <li>and then runs the operation,</li>
         * <li>and then notifies the blocked lookup to proceed.</li>
         *
         * @param runnable the runnable to execute
         * @return the termination interface
         */
        public Termination proceed(Runnable runnable) {
            try {
                final long delay = timeout.get(ChronoUnit.SECONDS);
                monitor.passivator.log.debug("waiting " + delay + "s for passivation");
                boolean passivated = monitor.passivated.await(delay, TimeUnit.SECONDS);
                if (!enforce || passivated) {
                    monitor.passivator.log.debug("proceeding");
                    ClassLoader tcl = Thread.currentThread()
                            .getContextClassLoader();
                    try {
                        runnable.run();
                    } finally {
                        Thread.currentThread()
                                .setContextClassLoader(tcl);
                    }
                }
                return monitor.passivator.accounting.last
                        .<Termination> map(Failure::new)
                        .orElseGet(Success::new);
            } catch (InterruptedException cause) {
                Thread.currentThread()
                        .interrupt();
                throw new AssertionError("Interrupted while waiting for passivation", cause);
            } finally {
                monitor.cancel();
            }
        }

    }

    /**
     * Terminates the pacification by a success or a failure action and release the lock.
     */
    public interface Termination {

        /**
         * Executes the runnable if the passivation was as success
         *
         * @param runnable
         */
        default Termination onSuccess(Runnable finisher) {
            return this;
        }

        /**
         * Recover the failure if the passivation was a failure, ie: some activity has been detected during the
         * protected operation.
         *
         * @param runnable the failure action
         */
        default Termination onFailure(Consumer<InScopeOfContext> recoverer) {
            return this;
        }

        default Termination peek(Consumer<Termination> consumer) {
            consumer.accept(this);
            return this;
        }

        class Success implements Termination {

            @Override
            public Termination onSuccess(Runnable finisher) {
                finisher.run();
                return this;
            }

        }

        class Failure implements Termination {

            Failure(InScopeOfContext snapshot) {
                this.snapshot = snapshot;
            }

            final InScopeOfContext snapshot;

            @Override
            public Termination onFailure(Consumer<InScopeOfContext> recoverer) {
                recoverer.accept(snapshot);
                return this;
            }
        }
    }

    /**
     * Intercepts service lookups for blocking other threads.
     */
    static class PassivateProvider implements ServiceProvider {

        PassivateProvider(Thread ownerThread, Accounting accounting, ServiceProvider waitfor,
                ServiceProvider passthrough) {
            this.ownerThread = ownerThread;
            this.accounting = accounting;
            this.waitfor = waitfor;
            this.passthrough = passthrough;
        }

        final Thread ownerThread;

        final Accounting accounting;

        final ServiceProvider passthrough;

        final ServiceProvider waitfor;

        @Override
        public <T> T getService(Class<T> typeof) {
            if (Thread.currentThread() == ownerThread) {
                return passthrough.getService(typeof);
            }
            return accounting
                    .take(typeof)
                    .map(snapshot -> passthrough)
                    .orElse(waitfor)
                    .getService(typeof);
        }
    }

    /**
     * Delegates the lookup to the previously installed service provider.
     */
    static class DelegateProvider implements ServiceProvider {

        DelegateProvider(ServiceProvider provider) {
            next = provider;
        }

        final ServiceProvider next;

        @Override
        public <T> T getService(Class<T> serviceClass) {
            return next.getService(serviceClass);
        }

    }

    /**
     * Let runtime resolve the service.
     */
    static class RuntimeProvider implements ServiceProvider {

        @Override
        public <T> T getService(Class<T> serviceClass) {
            return Framework.getRuntime()
                    .getService(serviceClass);
        }

    }

    /**
     * Waits for the condition before invoking the effective lookup.
     */
    static class WaitForProvider implements ServiceProvider {

        WaitForProvider(CountDownLatch condition, ServiceProvider passthrough) {
            this.condition = condition;
            this.passthrough = passthrough;
        }

        final CountDownLatch condition;

        final ServiceProvider passthrough;

        @Override
        public <T> T getService(Class<T> serviceClass) {
            try {
                condition.await();
            } catch (InterruptedException error) {
                Thread.currentThread()
                        .interrupt();
                throw new AssertionError("Interrupted while waiting for " + serviceClass);
            }
            return passthrough.getService(serviceClass);
        }

    }

}
