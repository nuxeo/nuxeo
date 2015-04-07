/*
 * Copyright (c) 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.test;

import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.storage.sql.DatabaseDB2;
import org.nuxeo.ecm.core.storage.sql.DatabaseDerby;
import org.nuxeo.ecm.core.storage.sql.DatabaseH2;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.core.storage.sql.DatabaseMySQL;
import org.nuxeo.ecm.core.storage.sql.DatabaseOracle;
import org.nuxeo.ecm.core.storage.sql.DatabasePostgreSQL;
import org.nuxeo.ecm.core.storage.sql.DatabaseSQLServer;
import org.nuxeo.runtime.api.Framework;

/**
 * Description of the specific capabilities of a repository for tests, and helper methods.
 *
 * @since 7.3
 */
public class StorageConfiguration {

    private DatabaseHelper databaseHelper = DatabaseHelper.DATABASE;

    public String getRepositoryName() {
        return databaseHelper.repositoryName;
    }

    public String getVCSName() {
        String db = databaseHelper.getClass().getSimpleName();
        if (db.startsWith("Database")) {
            db = db.substring("Database".length());
        }
        return db;
    }

    public boolean isVCSH2() {
        return databaseHelper instanceof DatabaseH2;
    }

    public boolean isVCSDerby() {
        return databaseHelper instanceof DatabaseDerby;
    }

    public boolean isVCSPostgreSQL() {
        return databaseHelper instanceof DatabasePostgreSQL;
    }

    public boolean isVCSMySQL() {
        return databaseHelper instanceof DatabaseMySQL;
    }

    public boolean isVCSOracle() {
        return databaseHelper instanceof DatabaseOracle;
    }

    public boolean isVCSSQLServer() {
        return databaseHelper instanceof DatabaseSQLServer;
    }

    public boolean isVCSDB2() {
        return databaseHelper instanceof DatabaseDB2;
    }

    /**
     * For databases that do asynchronous fulltext indexing, sleep a bit.
     */
    public void sleepForFulltext() {
        databaseHelper.sleepForFulltext();
    }

    /**
     * For databases that don't have sub-second resolution, sleep a bit to get to the next second.
     */
    public void maybeSleepToNextSecond() {
        databaseHelper.maybeSleepToNextSecond();
    }

    /**
     * Checks if the database has sub-second resolution.
     */
    public boolean hasSubSecondResolution() {
        return databaseHelper.hasSubSecondResolution();
    }

    public void waitForAsyncCompletion() {
        Framework.getService(EventService.class).waitForAsyncCompletion();
    }

    public void waitForFulltextIndexing() {
        waitForAsyncCompletion();
        sleepForFulltext();
    }

    /**
     * Checks if the database supports multiple fulltext indexes.
     */
    public boolean supportsMultipleFulltextIndexes() {
        return databaseHelper.supportsMultipleFulltextIndexes();
    }

}
