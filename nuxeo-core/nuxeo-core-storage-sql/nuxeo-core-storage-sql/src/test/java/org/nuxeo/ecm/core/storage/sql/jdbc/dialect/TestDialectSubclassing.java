/*
 * (C) Copyright 2012-2017 Nuxeo (http://nuxeo.com/) and others.
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
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.jdbc.dialect;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestDialectSubclassing {

    protected DatabaseMetaData metadata;

    protected Connection connection;

    protected RepositoryDescriptor repositoryDescriptor;

    protected Mockery jmcontext = new JUnit4Mockery();

    @Before
    public void setUp() throws Exception {
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
        public DialectDummy(DatabaseMetaData metadata, RepositoryDescriptor repositoryDescriptor) {
            super(metadata, repositoryDescriptor);
        }
    }

    @Test
    public void testDialectSubclassing() throws Exception {
        Framework.getProperties().setProperty(Dialect.DIALECT_CLASS, DialectDummy.class.getName());
        Dialect dialect = Dialect.createDialect(connection, repositoryDescriptor);
        assertEquals(DialectDummy.class, dialect.getClass());
    }

    @Test
    public void testDialectSubclassingSpecific() throws Exception {
        Framework.getProperties().setProperty(Dialect.DIALECT_CLASS + ".Dummy", DialectDummy.class.getName());
        Dialect dialect = Dialect.createDialect(connection, repositoryDescriptor);
        assertEquals(DialectDummy.class, dialect.getClass());
    }

}
