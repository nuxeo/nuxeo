package org.nuxeo.elasticsearch.api;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.elasticsearch.commands.IndexingCommand;

public interface ElasticSearchIndexing {

    void indexNow(IndexingCommand cmd) throws ClientException;

    void indexNow(List<IndexingCommand> cmds) throws ClientException;

    void scheduleIndexing(IndexingCommand cmd) throws ClientException;

    void flush();

    void flush(boolean commit);
}
