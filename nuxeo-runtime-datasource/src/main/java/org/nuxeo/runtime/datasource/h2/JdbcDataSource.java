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
package org.nuxeo.runtime.datasource.h2;

import java.sql.SQLException;

import javax.sql.XAConnection;

import org.h2.jdbcx.JdbcXAConnection;

public class JdbcDataSource extends org.h2.jdbcx.JdbcDataSource {

    private static final long serialVersionUID = 1L;

    public JdbcDataSource() {
       super();
    }

    @Override
    public XAConnection getXAConnection() throws SQLException {
        return XAConnectionRollbackHandler.newProxy((JdbcXAConnection)super.getXAConnection());
    }
}
