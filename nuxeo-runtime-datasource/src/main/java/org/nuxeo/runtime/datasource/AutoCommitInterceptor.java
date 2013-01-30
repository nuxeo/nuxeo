/*******************************************************************************
 * Copyright (c) 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *******************************************************************************/
package org.nuxeo.runtime.datasource;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.apache.tomcat.jdbc.pool.PooledConnection;
import org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class AutoCommitInterceptor extends ConnectionState {

    protected final Log log = LogFactory.getLog(AutoCommitInterceptor.class);

    @Override
    public void reset(ConnectionPool parent, PooledConnection con) {
        super.reset(parent, con);
        if (parent == null || con == null) {
            return;
        }
        Connection connection = con.getConnection();
        boolean autocommit = !TransactionHelper.isTransactionActive();
        try {
            if (connection.getAutoCommit() != autocommit) {
                connection.setAutoCommit(autocommit);
            }
        } catch (SQLException x) {
            autoCommit = autocommit;
            log.error("Unable to reset autocommit state to " + autocommit, x);
        }
    }

}
