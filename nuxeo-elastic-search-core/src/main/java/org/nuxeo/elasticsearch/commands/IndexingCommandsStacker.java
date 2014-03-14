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

package org.nuxeo.elasticsearch.commands;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED_BY_COPY;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_MOVED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_REMOVED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_SECURITY_UPDATED;

import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.elasticsearch.listener.EventConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * Contains logic to stack ElasticSearch commands depending on Document events
 *
 * This class is mainly here to make testing easier
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
public abstract class IndexingCommandsStacker {

    protected abstract Map<String, IndexingCommands> getAllCommands();

    protected IndexingCommands getCommands(DocumentModel doc) {
        return getAllCommands().get(doc.getId());
    }

    protected IndexingCommands getOrCreateCommands(DocumentModel doc) {
        IndexingCommands cmds = getCommands(doc);
        if (cmds == null) {
            cmds = new IndexingCommands(doc);
            getAllCommands().put(doc.getId(), cmds);
        }
        return cmds;
    }

    protected void stackCommand(DocumentModel doc, String eventId, boolean sync) {

        IndexingCommands cmds = getOrCreateCommands(doc);

        if (DOCUMENT_CREATED.equals(eventId)) {
            cmds.add(IndexingCommand.INDEX, sync, false);
        } else if (BEFORE_DOC_UPDATE.equals(eventId)) {
            cmds.add(IndexingCommand.UPDATE, sync, false);
        } else if (DOCUMENT_CREATED_BY_COPY.equals(eventId)) {
            cmds.add(IndexingCommand.INDEX, sync, doc.isFolder());
        } else if (DOCUMENT_MOVED.equals(eventId)) {
            cmds.add(IndexingCommand.UPDATE, sync, doc.isFolder());
        } else if (DOCUMENT_SECURITY_UPDATED.equals(eventId)) {
            cmds.add(IndexingCommand.UPDATE_SECURITY, sync, doc.isFolder());
        } else if (DOCUMENT_REMOVED.equals(eventId)) {
            cmds.add(IndexingCommand.DELETE, sync, doc.isFolder());
        }
    }

    protected void flushCommands(CoreSession session) throws ClientException {
        Map<String, IndexingCommands> allCmds = getAllCommands();
        EventProducer evtProducer = Framework.getLocalService(EventProducer.class);

        for (IndexingCommands cmds : allCmds.values()) {

            DocumentModel target = cmds.getTargetDocument();
            DocumentEventContext docCtx = new DocumentEventContext(session,
                    session.getPrincipal(), target);
            for (IndexingCommand cmd : cmds.getMergedCommands()) {

                Event evt = null;
                if (cmd.isSync()) {
                    evt = docCtx.newEvent(EventConstants.ES_INDEX_EVENT_SYNC);
                } else {
                    evt = docCtx.newEvent(EventConstants.ES_INDEX_EVENT);
                }

                evt.getContext().getProperties().put(
                        EventConstants.ES_RECURSE_FLAG, cmd.isRecurse());

                evtProducer.fireEvent(evt);
            }
        }
        getAllCommands().clear();
    }


}