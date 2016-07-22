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

import javax.transaction.TransactionManager;

import org.apache.geronimo.connector.outbound.AbstractConnectionManager;
import org.apache.geronimo.connector.outbound.ConnectionHandleInterceptor;
import org.apache.geronimo.connector.outbound.ConnectionInterceptor;
import org.apache.geronimo.connector.outbound.ConnectionTrackingInterceptor;
import org.apache.geronimo.connector.outbound.GenericConnectionManager;
import org.apache.geronimo.connector.outbound.MCFConnectionInterceptor;
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
 * Setups a connection according to the pooling attributes, mainly duplicated
 * from {@link GenericConnectionManager} for injecting a connection validation
 * interceptor.
 *
 * @since 8.3
 */
public class NuxeoConnectionManager extends AbstractConnectionManager {
    private static final long serialVersionUID = 1L;
    protected static final Logger log = LoggerFactory.getLogger(NuxeoConnectionManager.class);

    public NuxeoConnectionManager(NuxeoValidationSupport validationSupport,
            TransactionSupport transactionSupport,
            PoolingSupport pooling,
            SubjectSource subjectSource,
            ConnectionTracker connectionTracker,
            RecoverableTransactionManager transactionManager,
            String name,
            ClassLoader classLoader) {
        super(new InterceptorsImpl(validationSupport, transactionSupport, pooling, subjectSource, name, connectionTracker, transactionManager,
                classLoader),
                transactionManager, name);
    }

    static class InterceptorsImpl implements AbstractConnectionManager.Interceptors {

        private final ConnectionInterceptor stack;
        private final ConnectionInterceptor recoveryStack;
        private final PoolingSupport poolingSupport;

        /**
         * Order of constructed interceptors:
         * <p/>
         * ConnectionTrackingInterceptor (connectionTracker != null)
         * TCCLInterceptor ConnectionHandleInterceptor
         * ValidationHandleInterceptor TransactionCachingInterceptor
         * (useTransactions & useTransactionCaching)
         * TransactionEnlistingInterceptor (useTransactions) SubjectInterceptor
         * (realmBridge != null) SinglePoolConnectionInterceptor or
         * MultiPoolConnectionInterceptor LocalXAResourceInsertionInterceptor or
         * XAResourceInsertionInterceptor (useTransactions (&localTransactions))
         * MCFConnectionInterceptor
         */
        public InterceptorsImpl(NuxeoValidationSupport validationSupport, TransactionSupport transactionSupport,
                PoolingSupport pooling,
                SubjectSource subjectSource,
                String name,
                ConnectionTracker connectionTracker,
                TransactionManager transactionManager,
                ClassLoader classLoader) {
            // check for consistency between attributes
            if (subjectSource == null && pooling instanceof PartitionedPool && ((PartitionedPool) pooling).isPartitionBySubject()) {
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
                stack = new ConnectionTrackingInterceptor(stack,
                        name,
                        connectionTracker);
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

}
