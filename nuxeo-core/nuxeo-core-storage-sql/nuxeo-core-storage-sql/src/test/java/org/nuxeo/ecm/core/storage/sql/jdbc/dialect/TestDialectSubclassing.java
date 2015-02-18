/*
 * Copyright (c) 2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql.jdbc.dialect;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestDialectSubclassing extends NXRuntimeTestCase {

    protected DatabaseMetaData metadata;

    protected Connection connection;

    protected RepositoryDescriptor repositoryDescriptor;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        metadata = getDatabaseMetaData();
        connection = getConnection();
        repositoryDescriptor = new RepositoryDescriptor();
    }

    protected DatabaseMetaData getDatabaseMetaData() throws SQLException {
        final DatabaseMetaData m = jmcontext.mock(DatabaseMetaData.class);
        jmcontext.checking(new Expectations() {
            {
                oneOf(m).storesUpperCaseIdentifiers();
                will(returnValue(false));
                oneOf(m).getDatabaseProductName();
                will(returnValue("Dummy"));
            }
        });
        return m;
    }

    protected Connection getConnection() throws SQLException {
        final Connection m = jmcontext.mock(Connection.class);
        jmcontext.checking(new Expectations() {
            {
                oneOf(m).getMetaData();
                will(returnValue(metadata));
            }
        });
        return m;
    }

    protected static class DialectDummy extends DialectH2 {
        public DialectDummy(DatabaseMetaData metadata, RepositoryDescriptor repositoryDescriptor)
                throws StorageException {
            super(metadata, repositoryDescriptor);
        }
    }

    @Test
    public void testDialectSubclassing() throws Exception {
        Framework.getProperties().put(Dialect.DIALECT_CLASS, DialectDummy.class.getName());
        Dialect dialect = Dialect.createDialect(connection, repositoryDescriptor);
        assertEquals(DialectDummy.class, dialect.getClass());
    }

    @Test
    public void testDialectSubclassingSpecific() throws Exception {
        Framework.getProperties().put(Dialect.DIALECT_CLASS + ".Dummy", DialectDummy.class.getName());
        Dialect dialect = Dialect.createDialect(connection, repositoryDescriptor);
        assertEquals(DialectDummy.class, dialect.getClass());
    }

}
