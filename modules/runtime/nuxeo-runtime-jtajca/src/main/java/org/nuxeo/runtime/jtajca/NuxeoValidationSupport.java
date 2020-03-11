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
import javax.resource.spi.ManagedConnection;

import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.connector.outbound.ConnectionInfo;
import org.apache.geronimo.connector.outbound.ConnectionInterceptor;
import org.apache.geronimo.connector.outbound.ConnectionReturnAction;
import org.apache.geronimo.connector.outbound.ManagedConnectionInfo;
import org.tranql.connector.AbstractManagedConnection;
import org.tranql.connector.jdbc.ConnectionHandle;

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

    public interface Validation {
        boolean validate(ManagedConnection mc);
    }

    static final Validation NOOP = new Validation() {
        @Override
        public boolean validate(ManagedConnection mc) {
            return true;
        }
    };

    public static class ValidSQLConnection implements Validation {
        @Override
        public boolean validate(ManagedConnection mc) {
            try {
                @SuppressWarnings("unchecked")
                AbstractManagedConnection<Connection, ConnectionHandle> jdbcManagedConnection = (AbstractManagedConnection<Connection, ConnectionHandle>) mc;
                return jdbcManagedConnection.getPhysicalConnection().isValid(0);
            } catch (SQLException cause) {
                return false;
            }
        }
    }

    public static class QuerySQLConnection implements Validation {
        final String sql;

        QuerySQLConnection(String sql) {
            this.sql = sql;
        }

        @Override
        public boolean validate(ManagedConnection mc) {
            @SuppressWarnings("unchecked")
            AbstractManagedConnection<Connection, ConnectionHandle> jdbcManagedConnection = (AbstractManagedConnection<Connection, ConnectionHandle>) mc;
            try (Statement statement = jdbcManagedConnection.getPhysicalConnection().createStatement()) {
                return statement.execute(sql);
            } catch (SQLException cause) {
                LogFactory.getLog(QuerySQLConnection.class)
                        .warn(String.format("Caught error executing '%s', invalidating", sql), cause);
                return false;
            }
        }
    }

    public ConnectionInterceptor addValidationInterceptors(ConnectionInterceptor stack) {
        if (onBorrow == NOOP && onReturn == NOOP) {
            return stack;
        }
        return new ValidationInterceptor(stack);
    }

    class ValidationInterceptor implements ConnectionInterceptor {

        public ValidationInterceptor(ConnectionInterceptor next) {
            this.next = next;
        }

        final ConnectionInterceptor next;

        @Override
        public void getConnection(ConnectionInfo ci) throws ResourceException {
            while (true) {
                // request for a connection
                next.getConnection(ci);
                // validate connection
                if (onBorrow.validate(ci.getManagedConnectionInfo().getManagedConnection())) {
                    return;
                }
                // destroy invalid connection and retry
                LogFactory.getLog(NuxeoValidationSupport.class).warn("Returning invalid connection " + ci);
                returnConnection(ci, ConnectionReturnAction.DESTROY);
            }
        }

        @Override
        public void returnConnection(ConnectionInfo info, ConnectionReturnAction returnAction) {
            if (returnAction == ConnectionReturnAction.RETURN_HANDLE) {
                if (!onReturn.validate(info.getManagedConnectionInfo().getManagedConnection())) {
                    returnAction = ConnectionReturnAction.DESTROY;
                }
            }
            try {
                next.returnConnection(info, returnAction);
            } finally {
                if (returnAction == ConnectionReturnAction.DESTROY) {
                    // recycle managed connection info for a new managed connection
                    ManagedConnectionInfo mci = info.getManagedConnectionInfo();
                    mci = new ManagedConnectionInfo(mci.getManagedConnectionFactory(), mci.getConnectionRequestInfo());
                    info.setManagedConnectionInfo(mci);
                }
            }
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
