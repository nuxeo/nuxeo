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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnectionFactory;

import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.connector.outbound.ConnectionInfo;
import org.apache.geronimo.connector.outbound.ConnectionInterceptor;
import org.apache.geronimo.connector.outbound.ConnectionReturnAction;
import org.apache.geronimo.connector.outbound.ManagedConnectionInfo;

/**
 *
 *
 * @since 8.3
 */
public class NuxeoValidationSupport {

    final Validation onBorrow;

    final Validation onReturn;

    NuxeoValidationSupport(Validation onBorrow, Validation onReturn) {
        this.onBorrow = onBorrow == null ? NOOP : onBorrow;
        this.onReturn = onReturn == null ? NOOP : onReturn;
    }

    interface Validation {
        boolean validate(Object handle);

    }

    static final Validation NOOP = new Validation() {
        @Override
        public boolean validate(Object handle) {
            return true;
        }
    };

    static class ValidSQLConnection implements Validation {
        @Override
        public boolean validate(Object handle) {
            try {
                return ((Connection) handle).isValid(0);
            } catch (SQLException cause) {
                return false;
            }
        }
    }

    static class QuerySQLConnection implements Validation {
        final String sql;

        QuerySQLConnection(String sql) {
            this.sql = sql;
        }

        @Override
        public boolean validate(Object handle) {
            try (Statement statement = ((Connection) handle).unwrap(Connection.class).createStatement()) {
                return statement.execute(sql);
            } catch (SQLException cause) {
                LogFactory.getLog(QuerySQLConnection.class).warn(String.format("Caught error executing '%s', invalidating", sql), cause);
                return false;
            }
        }
    }

    public ConnectionInterceptor addTransactionInterceptor(ConnectionInterceptor stack) {
        if (onBorrow == NOOP && onReturn == NOOP) {
            return stack;
        }
        return new ValidationHandleInterceptor(stack);
    }

    class ValidationHandleInterceptor implements ConnectionInterceptor {

        public ValidationHandleInterceptor(ConnectionInterceptor next) {
            this.next = next;
        }

        final ConnectionInterceptor next;

        @Override
        public void getConnection(ConnectionInfo ci) throws ResourceException {
            ManagedConnectionInfo mci = ci.getManagedConnectionInfo();
            ManagedConnectionFactory mcf = mci.getManagedConnectionFactory();
            ConnectionRequestInfo cri = mci.getConnectionRequestInfo();
            while (true) {
                // request for a connection
                ConnectionInfo tryee = new ConnectionInfo(new ManagedConnectionInfo(mcf, cri));
                next.getConnection(tryee);
                // validate connection
                Object handle = tryee.getConnectionProxy();
                if (handle == null) {
                    handle = tryee.getConnectionHandle();
                }
                if (onBorrow.validate(handle)) {
                    // save handle an return connection
                    if (tryee.getConnectionProxy() != null) {
                        ci.setConnectionProxy(handle);
                    } else {
                        ci.setConnectionHandle(handle);
                    }
                    return;
                }
                // destroy invalid connection and retry
                LogFactory.getLog(NuxeoValidationSupport.class).error("Returning invalid connection " + ci);
                returnConnection(tryee, ConnectionReturnAction.DESTROY);
            }
        }

        @Override
        public void returnConnection(ConnectionInfo info, ConnectionReturnAction returnAction) {
            if (returnAction == ConnectionReturnAction.RETURN_HANDLE) {
                if (!onReturn.validate(info.getConnectionHandle())) {
                    returnAction = ConnectionReturnAction.DESTROY;
                }
            }
            next.returnConnection(info, returnAction);
        }

        @Override
        public void info(StringBuilder s) {
            next.info(s);
        }

        @Override
        public void destroy() {
            next.destroy();
        }

    }

}
