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

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import org.jmock.Mock;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.BinaryManager;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestDialectSubclassing extends NXRuntimeTestCase {

    protected DatabaseMetaData metadata;

    protected Connection connection;

    protected BinaryManager binaryManager;

    protected RepositoryDescriptor repositoryDescriptor;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        metadata = getDatabaseMetaData();
        connection = getConnection();
        binaryManager = null;
        repositoryDescriptor = new RepositoryDescriptor();
    }

    protected DatabaseMetaData getDatabaseMetaData() {
        Mock m = mock(DatabaseMetaData.class);
        m.stubs().method("storesUpperCaseIdentifiers").will(returnValue(false));
        m.stubs().method("getDatabaseProductName").will(returnValue("Dummy"));
        return (DatabaseMetaData) m.proxy();
    }

    protected Connection getConnection() {
        Mock m = mock(Connection.class);
        m.stubs().method("getMetaData").will(returnValue(metadata));
        return (Connection) m.proxy();
    }

    protected static class DialectDummy extends DialectH2 {
        public DialectDummy(DatabaseMetaData metadata,
                BinaryManager binaryManager,
                RepositoryDescriptor repositoryDescriptor)
                throws StorageException {
            super(metadata, binaryManager, repositoryDescriptor);
        }
    }

    public void testDialectSubclassing() throws Exception {
        Framework.getProperties().put(Dialect.DIALECT_CLASS,
                DialectDummy.class.getName());
        Dialect dialect = Dialect.createDialect(connection, binaryManager,
                repositoryDescriptor);
        assertEquals(DialectDummy.class, dialect.getClass());
    }

    public void testDialectSubclassingSpecific() throws Exception {
        Framework.getProperties().put(Dialect.DIALECT_CLASS + ".Dummy",
                DialectDummy.class.getName());
        Dialect dialect = Dialect.createDialect(connection, binaryManager,
                repositoryDescriptor);
        assertEquals(DialectDummy.class, dialect.getClass());
    }

}
