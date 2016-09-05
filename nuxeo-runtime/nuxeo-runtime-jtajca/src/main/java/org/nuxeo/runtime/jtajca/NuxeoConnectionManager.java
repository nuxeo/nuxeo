/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.runtime.jtajca;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import javax.resource.ResourceException;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.connector.outbound.AbstractConnectionManager;
import org.apache.geronimo.connector.outbound.ConnectionHandleInterceptor;
import org.apache.geronimo.connector.outbound.ConnectionInfo;
import org.apache.geronimo.connector.outbound.ConnectionInterceptor;
import org.apache.geronimo.connector.outbound.ConnectionReturnAction;
import org.apache.geronimo.connector.outbound.ConnectionTrackingInterceptor;
import org.apache.geronimo.connector.outbound.GenericConnectionManager;
import org.apache.geronimo.connector.outbound.MCFConnectionInterceptor;
import org.apache.geronimo.connector.outbound.PoolIdleReleaserTimer;
import org.apache.geronimo.connector.outbound.SubjectInterceptor;
import org.apache.geronimo.connector.outbound.SubjectSource;
import org.apache.geronimo.connector.outbound.TCCLInterceptor;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.PartitionedPool;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.PoolingSupport;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.TransactionSupport;
import org.apache.geronimo.connector.outbound.connectiontracking.ConnectionTracker;
import org.apache.geronimo.transaction.manager.RecoverableTransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Setups a connection according to the pooling attributes, mainly duplicated from {@link GenericConnectionManager} for
 * injecting a connection validation interceptor.
 *
 * @since 8.3
 */
public class NuxeoConnectionManager extends AbstractConnectionManager {
    private static final long serialVersionUID = 1L;
    protected static final Logger log = LoggerFactory.getLogger(NuxeoConnectionManager.class);

    final NuxeoConnectionTrackingCoordinator coordinator;

    public NuxeoConnectionManager(int activettl, NuxeoValidationSupport validationSupport,
            TransactionSupport transactionSupport, PoolingSupport pooling, SubjectSource subjectSource,
            NuxeoConnectionTrackingCoordinator connectionTracker, RecoverableTransactionManager transactionManager,
            String name, ClassLoader classLoader) {
        super(new InterceptorsImpl(validationSupport, transactionSupport, pooling, subjectSource, name,
                connectionTracker, transactionManager, classLoader), transactionManager, name);
        coordinator = connectionTracker;
        activemonitor = new ActiveMonitor(activettl);
        nosharing = new Nosharing();
    }

    static class InterceptorsImpl implements AbstractConnectionManager.Interceptors {

        private final ConnectionInterceptor stack;
        private final ConnectionInterceptor recoveryStack;
        private final PoolingSupport poolingSupport;

        /**
         * Order of constructed interceptors:
         * <p/>
         * ConnectionTrackingInterceptor (connectionTracker != null) TCCLInterceptor ConnectionHandleInterceptor
         * ValidationHandleInterceptor TransactionCachingInterceptor (useTransactions & useTransactionCaching)
         * TransactionEnlistingInterceptor (useTransactions) SubjectInterceptor (realmBridge != null)
         * SinglePoolConnectionInterceptor or MultiPoolConnectionInterceptor LocalXAResourceInsertionInterceptor or
         * XAResourceInsertionInterceptor (useTransactions (&localTransactions)) MCFConnectionInterceptor
         */
        public InterceptorsImpl(NuxeoValidationSupport validationSupport, TransactionSupport transactionSupport,
                PoolingSupport pooling, SubjectSource subjectSource, String name, ConnectionTracker connectionTracker,
                TransactionManager transactionManager, ClassLoader classLoader) {
            // check for consistency between attributes
            if (subjectSource == null && pooling instanceof PartitionedPool
                    && ((PartitionedPool) pooling).isPartitionBySubject()) {
                throw new IllegalStateException("To use Subject in pooling, you need a SecurityDomain");
            }

            // Set up the interceptor stack
            MCFConnectionInterceptor tail = new MCFConnectionInterceptor();
            ConnectionInterceptor stack = tail;

            stack = transactionSupport.addXAResourceInsertionInterceptor(stack, name);
            stack = pooling.addPoolingInterceptors(stack);
            if (log.isTraceEnabled()) {
                log.trace("Connection Manager " + name + " installed pool " + stack);
            }

            poolingSupport = pooling;
            stack = transactionSupport.addTransactionInterceptors(stack, transactionManager);

            if (subjectSource != null) {
                stack = new SubjectInterceptor(stack, subjectSource);
            }

            if (transactionSupport.isRecoverable()) {
                recoveryStack = new TCCLInterceptor(stack, classLoader);
            } else {
                recoveryStack = null;
            }

            stack = new ConnectionHandleInterceptor(stack);
            stack = validationSupport.addTransactionInterceptor(stack);
            stack = new TCCLInterceptor(stack, classLoader);
            if (connectionTracker != null) {
                stack = new ConnectionTrackingInterceptor(stack, name, connectionTracker);
            }
            tail.setStack(stack);
            this.stack = stack;
            if (log.isDebugEnabled()) {
                StringBuilder s = new StringBuilder("ConnectionManager Interceptor stack;\n");
                stack.info(s);
                log.debug(s.toString());
            }
        }

        @Override
        public ConnectionInterceptor getStack() {
            return stack;
        }

        @Override
        public ConnectionInterceptor getRecoveryStack() {
            return recoveryStack;
        }

        @Override
        public PoolingSupport getPoolingAttributes() {
            return poolingSupport;
        }

    }

    @Override
    public void doStop() throws Exception {
        if (getConnectionCount() < getPartitionMinSize()) {
            Thread.sleep(10); // wait for filling tasks completion
        }
        super.doStop();
    }

    /**
     *
     * @see ActiveMonitor#killTimedoutConnections
     * @since 8.4
     */
    public List<ActiveMonitor.TimeToLive> killActiveTimedoutConnections(long clock) {
        return activemonitor.killTimedoutConnections(clock);
    }

    /**
     *
     * @see NuxeoConnectionTrackingCoordinator#k
     * @since 8.4
     */
    public long getKilledConnectionCount() {
        return activemonitor.killedCount;
    }

    final ActiveMonitor activemonitor;

    class ActiveMonitor implements ConnectionTracker {

        final int ttl;

        ActiveMonitor(int delay) {
            ttl = delay;
            if (ttl > 0) {
                scheduleCleanups();
            }
            coordinator.addTracker(this);
        }

        final Map<ConnectionInfo, TimeToLive> ttls = new HashMap<>();

        long killedCount = 0L;

        CleanupTask cleanup = new CleanupTask();

        class CleanupTask extends TimerTask {

            @Override
            public void run() {
                killActiveTimedoutConnections(System.currentTimeMillis());
            }

        }

        void cancelCleanups() {
            cleanup.cancel();
        }

        void scheduleCleanups() {
            PoolIdleReleaserTimer.getTimer().scheduleAtFixedRate(cleanup, 60 * 1000, 60 * 1000);
        }

        @Override
        public synchronized void handleObtained(ConnectionTrackingInterceptor connectionTrackingInterceptor,
                ConnectionInfo connectionInfo, boolean reassociate) throws ResourceException {
            int delay = ttl();
            if (delay > 0) {
                ttls.put(connectionInfo, new TimeToLive(connectionInfo, Thread.currentThread().getName(), System.currentTimeMillis(), delay));
            }
        }

        @Override
        public synchronized void handleReleased(ConnectionTrackingInterceptor connectionTrackingInterceptor,
                ConnectionInfo connectionInfo, ConnectionReturnAction connectionReturnAction) {
            ttls.remove(connectionInfo);
        }

        @Override
        public void setEnvironment(ConnectionInfo connectionInfo, String key) {
            return;
        }

        /**
         * Kill active connection that have timed out relative to the given {@code clock}.
         *
         * @return information about the killed connections
         * @param clock
         * @since 8.4
         */
        public synchronized List<TimeToLive> killTimedoutConnections(long clock) {
            List<TimeToLive> killed = new LinkedList<>();
            Iterator<TimeToLive> it = ttls.values().iterator();
            while (it.hasNext()) {
                TimeToLive ttl = it.next();
                if (ttl.deadline <= clock) {
                    ttl.killAndLog();
                    killed.add(ttl);
                    it.remove();
                }

            }
            return killed;
        }

        /**
         * Logs active connections
         *
         *
         * @since 8.4
         */
        public void log() {
            for (TimeToLive ttl : ttls.values()) {
                LogFactory.getLog(TimeToLive.class).warn(ttl, ttl.info.getTrace());
            }
        }

        public class TimeToLive {

            public final ConnectionInfo info;

            public final String threadName;

            public final long obtained;

            public final long deadline;

            TimeToLive(ConnectionInfo info, String threadName, long obtained, int delay) {
                this.info = info;
                this.threadName = threadName;
                this.obtained = System.currentTimeMillis();
                deadline = obtained + delay;
            }

            void killAndLog() {
                try {
                    info.getManagedConnectionInfo().getPoolInterceptor().returnConnection(info,
                            ConnectionReturnAction.DESTROY);
                } catch (Throwable error) {
                    if (error instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                        throw error;
                    }
                    LogFactory.getLog(NuxeoConnectionTrackingCoordinator.class)
                            .error("Caught error while killing " + info, error);
                } finally {
                    killedCount += 1;
                    LogFactory.getLog(NuxeoConnectionTrackingCoordinator.class)
                            .error("Killed " + message(new StringBuilder()), info.getTrace());
                }
            }

            void log(long clock) {
                if (deadline < clock) {
                    LogFactory.getLog(NuxeoConnectionTrackingCoordinator.class).info(message(new StringBuilder()),
                            info.getTrace());
                } else {
                    LogFactory.getLog(NuxeoConnectionTrackingCoordinator.class).error(message(new StringBuilder()),
                            info.getTrace());
                }
            }

            public StringBuilder message(StringBuilder builder) {
                return builder.append(info).append(",  was obtained by ").append(threadName).append(" at ")
                        .append(new Date(obtained)).append(" and timed out at ").append(new Date(deadline));
            }

            @Override
            public String toString() {
                return String.format("TimeToLive(%x) %s", hashCode(), message(new StringBuilder()).toString());
            }
        }

        final ThreadLocal<Integer> context = new ThreadLocal<>();

        public void enter(int delay) {
            context.set(delay);
        };

        public void exit() {
            context.remove();
        }

        int ttl() {
            Integer value = context.get();
            if (value != null) {
                return value.intValue();
            }
            return ttl;
        }
    }

    public int getActiveTimeoutMinutes() {
        return activemonitor.ttl / (60 * 1000);
    }

    public void enterActiveMonitor(int delay) {
        activemonitor.enter(delay);
    }

    public void exitActiveTimedout() {
        activemonitor.exit();
    }

    final Nosharing nosharing;

    class Nosharing implements ConnectionTracker {

        Nosharing() {
            coordinator.addTracker(this);
        }

        @Override
        public void handleObtained(ConnectionTrackingInterceptor connectionTrackingInterceptor,
                ConnectionInfo connectionInfo, boolean reassociate) throws ResourceException {

        }

        @Override
        public void handleReleased(ConnectionTrackingInterceptor connectionTrackingInterceptor,
                ConnectionInfo connectionInfo, ConnectionReturnAction connectionReturnAction) {

        }

        final ThreadLocal<Boolean> noSharingHolder = new ThreadLocal<Boolean>();

        @Override
        public void setEnvironment(ConnectionInfo connectionInfo, String key) {
            connectionInfo.setUnshareable(noSharingHolder.get() == null ? false : true);
        }

        void enter() {
            noSharingHolder.set(Boolean.TRUE);
        }

        void exit() {
            noSharingHolder.remove();
        }

    }

    public void enterNoSharing() {
        nosharing.enter();
    }

    public void exitNoSharing() {
        nosharing.exit();
    }

}
