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
 *
 */
package org.nuxeo.runtime.jtajca;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEventListener;

import org.apache.geronimo.connector.outbound.ConnectionInfo;
import org.apache.geronimo.connector.outbound.ConnectionInterceptor;
import org.apache.geronimo.connector.outbound.ConnectionReturnAction;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.SinglePool;

/**
 *
 *
 * @since 8.4
 */
public class NuxeoPool extends SinglePool {

    ConnectionEventListener listener;

    public NuxeoPool(NuxeoConnectionManagerConfiguration config) {
        super(config.getMaxPoolSize(), config.getMinPoolSize(), config.getBlockingTimeoutMillis(),
                config.getIdleTimeoutMinutes(), config.getMatchOne(), config.getMatchAll(),
                config.getSelectOneNoMatch());
    }

    private static final long serialVersionUID = 1L;

    void addInterceptor(ConnectionInterceptor interceptor) {

    }

    @Override
    public ConnectionInterceptor addPoolingInterceptors(ConnectionInterceptor tail) {
        ConnectionInterceptor stack = super.addPoolingInterceptors(tail);
        //
        return new ConnectionInterceptor() {

            @Override
            public void returnConnection(ConnectionInfo connectionInfo, ConnectionReturnAction connectionReturnAction) {
                stack.returnConnection(connectionInfo, connectionReturnAction);
            }

            @Override
            public void info(StringBuilder s) {
                stack.info(s);
            }

            @Override
            public void getConnection(ConnectionInfo connectionInfo) throws ResourceException {
                stack.getConnection(connectionInfo);
                // needed for killing connections
                connectionInfo.getManagedConnectionInfo().setPoolInterceptor(this);
            }

            @Override
            public void destroy() {
                stack.destroy();
            }
        };
    }

}
