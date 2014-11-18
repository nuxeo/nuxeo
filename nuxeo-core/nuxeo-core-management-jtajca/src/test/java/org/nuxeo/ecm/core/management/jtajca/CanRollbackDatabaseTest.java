package org.nuxeo.ecm.core.management.jtajca;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.naming.NamingException;
import javax.sql.DataSource;

import junit.framework.AssertionFailedError;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.runtime.datasource.DataSourceHelper;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.runtime.transaction.TransactionRuntimeException;

@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class })
public class CanRollbackDatabaseTest {

    @LocalDeploy("org.nuxeo.ecm.core.management:ds-contrib-with-fatal.xml")
    @Test(expected=TransactionRuntimeException.class)
    public void testFatalRollback() throws NamingException, SQLException {
        insertWrongReference();
    }

    @LocalDeploy("org.nuxeo.ecm.core.management:ds-contrib.xml")
    @Test(expected=SQLException.class)
    public void testNoFatalRollback() throws NamingException, SQLException {
        insertWrongReference();
    }

    private void insertWrongReference() throws NamingException, SQLException,
            AssertionFailedError {
        DataSource ds = DataSourceHelper.getDataSource("jdbc/repository_test");
        {
            try (Connection db = ds.getConnection()) {
                try (Statement st = db.createStatement()) {
                    st.execute("INSERT into hierarchy(id) values('pfouh')");
                    st.addBatch("INSERT into hierarchy (id, parentid ) values ('1','2')");
                    st.executeBatch();
                }
            } finally {
                try {
                    TransactionHelper.setTransactionRollbackOnly();
                    TransactionHelper.commitOrRollbackTransaction();
                } finally {
                    TransactionHelper.startTransaction();
                    try (Connection db = ds.getConnection()) {
                        try (Statement st = db.createStatement()) {
                            try (ResultSet rs = st
                                .executeQuery("SELECT id from hierarchy where id = 'pfouh'")) {
                                if (rs.next()) {
                                    throw new AssertionFailedError("connection was not rollbacked");
                                }
                            }
                        }
                    }
                }
            }
        }
    }


}
