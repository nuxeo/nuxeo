/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     tiry
 */
package org.nuxeo.elasticsearch.listener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.runtime.api.Framework;

public class ElasticSearchPostCommitSyncListener implements
        PostCommitEventListener {

    @Override
    public void handleEvent(EventBundle bundle) throws ClientException {

        List<IndexingCommand> cmds = new ArrayList<>();
        for (Event event : bundle) {
            if (EventConstants.ES_INDEX_EVENT_SYNC.equals(event.getName())) {
                Map<String, Serializable> props = event.getContext()
                        .getProperties();
                for (String key : props.keySet()) {
                    if (key.startsWith(IndexingCommand.PREFIX)) {
                        IndexingCommand cmd = IndexingCommand.fromJSON(event
                                .getContext().getCoreSession(), (String) props
                                .get(key));
                        cmds.add(cmd);
                    }
                }
            }
        }
        if (!cmds.isEmpty()) {
            ElasticSearchIndexing esi = Framework
                    .getLocalService(ElasticSearchIndexing.class);
            esi.indexNow(cmds);
            ElasticSearchAdmin esa = Framework
                    .getLocalService(ElasticSearchAdmin.class);
            esa.refresh();
        }
    }

}
