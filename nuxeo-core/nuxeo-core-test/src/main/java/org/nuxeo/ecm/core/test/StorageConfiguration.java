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

    public static final String CORE_PROPERTY = "nuxeo.test.core";

    public static final String CORE_VCS = "vcs";

    public static final String CORE_DBS_MEM = "mem";

    public static final String CORE_DBS_MONGODB = "mongodb";

    public static final String CORE_DEFAULT = CORE_VCS;

    private DatabaseHelper vcsDatabaseHelper;

    private String dbsBackend;

    public StorageConfiguration() {
        String core = defaultSystemProperty(CORE_PROPERTY, CORE_DEFAULT);
        switch (core) {
        case CORE_VCS:
            initVCS();
            break;
        case CORE_DBS_MEM:
        case CORE_DBS_MONGODB:
            dbsBackend = core;
            break;
        default:
            throw new ExceptionInInitializerError("Unknown test core mode: " + core);
        }
    }

    protected static String defaultSystemProperty(String name, String def) {
        String value = System.getProperty(name);
        if (value == null || value.equals("") || value.equals("${" + name + "}")) {
            System.setProperty(name, value = def);
        }
        return value;
    }

    protected void initVCS() {
        vcsDatabaseHelper = DatabaseHelper.DATABASE;
    }

    // used only for datasource contrib in TestSQLBinaryManager
    public String getVCSName() {
        String db = vcsDatabaseHelper.getClass().getSimpleName();
        if (db.startsWith("Database")) {
            db = db.substring("Database".length());
        }
        return db;
    }

    public boolean isVCS() {
        return vcsDatabaseHelper != null;
    }

    public boolean isVCSH2() {
        return vcsDatabaseHelper instanceof DatabaseH2;
    }

    public boolean isVCSDerby() {
        return vcsDatabaseHelper instanceof DatabaseDerby;
    }

    public boolean isVCSPostgreSQL() {
        return vcsDatabaseHelper instanceof DatabasePostgreSQL;
    }

    public boolean isVCSMySQL() {
        return vcsDatabaseHelper instanceof DatabaseMySQL;
    }

    public boolean isVCSOracle() {
        return vcsDatabaseHelper instanceof DatabaseOracle;
    }

    public boolean isVCSSQLServer() {
        return vcsDatabaseHelper instanceof DatabaseSQLServer;
    }

    public boolean isVCSDB2() {
        return vcsDatabaseHelper instanceof DatabaseDB2;
    }

    public boolean isDBS() {
        return dbsBackend != null;
    }

    public boolean isDBSMem() {
        return CORE_DBS_MEM.equals(dbsBackend);
    }

    public boolean isDBSMongoDB() {
        return CORE_DBS_MONGODB.equals(dbsBackend);
    }

    public String getRepositoryName() {
        if (isVCS()) {
            return vcsDatabaseHelper.repositoryName;
        } else {
            return "test"; // DBS
        }
    }

    /**
     * For databases that do asynchronous fulltext indexing, sleep a bit.
     */
    public void sleepForFulltext() {
        if (isVCS()) {
            vcsDatabaseHelper.sleepForFulltext();
        } else {
            // DBS
        }
    }

    /**
     * For databases that don't have sub-second resolution, sleep a bit to get to the next second.
     */
    public void maybeSleepToNextSecond() {
        if (isVCS()) {
            vcsDatabaseHelper.maybeSleepToNextSecond();
        } else {
            // DBS
        }
    }

    /**
     * Checks if the database has sub-second resolution.
     */
    public boolean hasSubSecondResolution() {
        if (isVCS()) {
            return vcsDatabaseHelper.hasSubSecondResolution();
        } else {
            return true; // DBS
        }
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
        if (isVCS()) {
            return vcsDatabaseHelper.supportsMultipleFulltextIndexes();
        } else {
            return false; // DBS
        }
    }

}
