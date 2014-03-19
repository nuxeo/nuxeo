/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.elasticsearch.api;

import java.util.List;

import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.service.PendingClusterTask;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.elasticsearch.config.NuxeoElasticSearchConfig;

/**
 * Administration interface for Elastic Search service
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
public interface ElasticSearchAdmin {

    /**
     * Retrieves the {@link Client} that can be used to acces ElasticSearch API
     *
     * @return
     */
    Client getClient();

    NuxeoElasticSearchConfig getConfig();

    void initIndexes(boolean recreate) throws Exception;

    boolean isAlreadyScheduledForIndexing(DocumentModel doc);

    int getPendingDocs();

    int getPendingCommands();

    List<PendingClusterTask> getPendingTasks();

}
