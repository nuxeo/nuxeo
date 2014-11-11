/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.core.test;

import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.DatabaseHelperFactory;

public class DefaultDatabaseFactory implements DatabaseHelperFactory {

    @Override
    public DatabaseHelper getHelper(BackendType type, String databaseName, String repositoryName) {
        // type is ignored, the config inferred by DatabaseHelper from
        // system properties will be used
        DatabaseHelper helper = DatabaseHelper.DATABASE;
        // helper.setDatabaseName(databaseName);
        helper.setRepositoryName(repositoryName);
        return helper;
    }

}
