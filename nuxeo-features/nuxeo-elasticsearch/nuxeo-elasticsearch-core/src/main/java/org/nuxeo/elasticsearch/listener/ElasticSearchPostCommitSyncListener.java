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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.runtime.api.Framework;

public class ElasticSearchPostCommitSyncListener implements PostCommitEventListener {

    @Override
    public void handleEvent(EventBundle bundle) throws ClientException {

        List<IndexingCommand> cmds = new ArrayList<>();
        Map<String, CoreSession> sessions = new HashMap<>();
        for (Event event : bundle) {
            if (EventConstants.ES_INDEX_EVENT_SYNC.equals(event.getName())) {

                Map<String, Serializable> props = event.getContext().getProperties();
                for (String key : props.keySet()) {
                    if (key.startsWith(IndexingCommand.PREFIX)) {
                        IndexingCommand cmd = IndexingCommand.fromJSON((String) props.get(key));
                        cmds.add(cmd);
                        sessions.put(cmd.getRepository(), null);
                    }
                }
            }
        }
        if (!cmds.isEmpty()) {
            ElasticSearchIndexing esi = Framework.getLocalService(ElasticSearchIndexing.class);
            try {
                attachSessions(sessions, cmds);
                esi.indexNow(cmds);
            } finally {
                closeSessions(sessions);
            }
            ElasticSearchAdmin esa = Framework.getLocalService(ElasticSearchAdmin.class);
            esa.refresh();
        }
    }

    protected void closeSessions(Map<String, CoreSession> sessions) {
        for (CoreSession session : sessions.values()) {
            if (session != null) {
                session.close();
            }
        }
    }

    protected void attachSessions(Map<String, CoreSession> sessions, List<IndexingCommand> cmds) {
        for (String repo : sessions.keySet()) {
            sessions.put(repo, CoreInstance.openCoreSession(repo));
        }
        for (IndexingCommand cmd : cmds) {
            cmd.attach(sessions.get(cmd.getRepository()));
        }
    }

}
