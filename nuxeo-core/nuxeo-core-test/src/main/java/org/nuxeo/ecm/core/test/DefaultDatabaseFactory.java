package org.nuxeo.ecm.core.test;

import org.nuxeo.ecm.core.storage.sql.DatabaseH2;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.core.storage.sql.DatabasePostgreSQL;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.DatabaseHelperFactory;

public class DefaultDatabaseFactory implements DatabaseHelperFactory {

    @Override
    public DatabaseHelper getHelper(BackendType type, String databaseName, String repositoryName) {
        DatabaseHelper helper;
        if (type == BackendType.H2) {
            helper = DatabaseH2.INSTANCE;
        } else if (type == BackendType.POSTGRES) {
            helper = DatabasePostgreSQL.INSTANCE;
        } else {
            throw new UnsupportedOperationException(type + " is not supported");
        }
        helper.setDatabaseName(databaseName);
        helper.setRepositoryName(repositoryName);
        return helper;
    }

}
