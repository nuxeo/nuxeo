package org.nuxeo.elasticsearch.listener;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.runtime.api.Framework;

public class ElasticsearchPostCommitSyncListener implements
        PostCommitEventListener {

    @Override
    public void handleEvent(EventBundle bundle) throws ClientException {

        ElasticSearchIndexing esi = Framework.getLocalService(ElasticSearchIndexing.class);
        boolean needFlush = false;
        for (Event event : bundle) {
            if (event.getName().equals(EventConstants.ES_INDEX_EVENT_SYNC)) {
                Map<String, Serializable> props = event.getContext().getProperties();
                for (String key : props.keySet()) {
                    if (key.startsWith(IndexingCommand.PREFIX)) {
                        IndexingCommand cmd = IndexingCommand.fromJSON(
                                event.getContext().getCoreSession(),
                                (String) props.get(key));
                        esi.indexNow(cmd);
                        needFlush = true;
                    }
                }
            }
        }
        if (needFlush) {
            // flush ES index
            esi.flush();
        }
    }

}
