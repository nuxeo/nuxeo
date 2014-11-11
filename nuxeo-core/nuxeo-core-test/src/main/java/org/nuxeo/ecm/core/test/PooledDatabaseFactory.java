/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     matic
 */
package org.nuxeo.ecm.core.test;

import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.core.test.DefaultDatabaseFactory;
import org.nuxeo.ecm.core.test.annotations.BackendType;

/**
 * @author matic
 *
 */
public class PooledDatabaseFactory extends DefaultDatabaseFactory {


    @Override
    public DatabaseHelper getHelper(BackendType type, String databaseName,
            String repositoryName) {
        DatabaseHelper helper = null;
        if (type == BackendType.H2) {
            helper = PooledH2Database.INSTANCE;
        } else if (type == BackendType.POSTGRES) {
            helper = PooledPostgresDatabase.INSTANCE;
        }

        if (helper == null) {
            throw new UnsupportedOperationException(type + " no pooled database helper available");
        }
        helper.setDatabaseName(databaseName);
        helper.setRepositoryName(repositoryName);
        return helper;
    }

}
