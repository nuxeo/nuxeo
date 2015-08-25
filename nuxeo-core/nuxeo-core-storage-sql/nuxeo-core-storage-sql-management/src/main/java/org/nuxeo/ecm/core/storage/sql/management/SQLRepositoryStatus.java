/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.management;

import java.util.List;

import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.binary.BinaryManagerStatus;
import org.nuxeo.ecm.core.storage.sql.RepositoryManagement;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLRepositoryService;
import org.nuxeo.runtime.api.Framework;

/**
 * An MBean to manage SQL storage repositories.
 */
public class SQLRepositoryStatus implements SQLRepositoryStatusMBean {

    protected static List<RepositoryManagement> getRepositories() {
        SQLRepositoryService sqlRepositoryService = Framework.getService(SQLRepositoryService.class);
        return sqlRepositoryService.getRepositories();
    }

    @Override
    public String listActiveSessions() {
        StringBuilder buf = new StringBuilder();
        buf.append("Actives sessions for SQL repositories:<br />");
        for (RepositoryManagement repository : getRepositories()) {
            buf.append("<b>").append(repository.getName()).append("</b>: ");
            buf.append(repository.getActiveSessionsCount());
            buf.append("<br />");
        }
        return buf.toString();
    }

    @Override
    public int getActiveSessionsCount() {
        int count = 0;
        for (RepositoryManagement repository : getRepositories()) {
            count += repository.getActiveSessionsCount();
        }
        return count;
    }

    @Override
    public String clearCaches() {
        StringBuilder buf = new StringBuilder();
        buf.append("Cleared cached objects for SQL repositories:<br />");
        for (RepositoryManagement repository : getRepositories()) {
            buf.append("<b>").append(repository.getName()).append("</b>: ");
            buf.append(repository.clearCaches());
            buf.append("<br />");
        }
        return buf.toString();
    }

    @Override
    public long getCachesSize() {
        long size = 0;
        for (RepositoryManagement repository : getRepositories()) {
            size += repository.getCacheSize();
        }
        return size;
    }

    @Override
    public String listRemoteSessions() {
        StringBuilder buf = new StringBuilder();
        buf.append("Actives remote session for SQL repositories:<br />");
        for (RepositoryManagement repository : getRepositories()) {
            buf.append("<b>").append(repository.getName()).append("</b>");
            buf.append("<br/>");
        }
        return buf.toString();
    }

    @Override
    public BinaryManagerStatus gcBinaries(boolean delete) {
        return Framework.getService(BlobManager.class).garbageCollectBinaries(delete);
    }

    @Override
    public boolean isBinariesGCInProgress() {
        return Framework.getService(BlobManager.class).isBinariesGarbageCollectionInProgress();
    }

}
