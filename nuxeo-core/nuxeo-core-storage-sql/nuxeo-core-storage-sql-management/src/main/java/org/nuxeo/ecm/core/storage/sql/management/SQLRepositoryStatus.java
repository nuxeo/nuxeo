/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.sql.management;

import java.util.List;

import org.nuxeo.ecm.core.blob.DocumentBlobManager;
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
        return Framework.getService(DocumentBlobManager.class).garbageCollectBinaries(delete);
    }

    @Override
    public boolean isBinariesGCInProgress() {
        return Framework.getService(DocumentBlobManager.class).isBinariesGarbageCollectionInProgress();
    }

}
