package org.nuxeo.elasticsearch.api;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.elasticsearch.commands.IndexingCommand;

public interface ElasticSearchIndexing {

    String indexNow(IndexingCommand cmd) throws ClientException;

    void scheduleIndexing(IndexingCommand cmd) throws ClientException;

    void flush();
}
