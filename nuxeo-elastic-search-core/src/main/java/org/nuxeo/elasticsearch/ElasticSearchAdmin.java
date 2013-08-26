package org.nuxeo.elasticsearch;

import org.nuxeo.ecm.core.api.DocumentModel;

public interface ElasticSearchAdmin {

    NuxeoElasticSearchConfig getConfig();

    boolean isAlreadyScheduledForIndexing(DocumentModel doc);

    int getPendingIndexingTasksCount();

}
