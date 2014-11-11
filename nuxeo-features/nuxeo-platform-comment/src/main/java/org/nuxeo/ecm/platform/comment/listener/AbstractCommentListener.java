/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.comment.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.comment.service.CommentServiceConfig;
import org.nuxeo.ecm.platform.comment.service.CommentServiceHelper;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.runtime.api.Framework;

public abstract class AbstractCommentListener {

     private static final Log log = LogFactory.getLog(AbstractCommentListener.class);

        public void handleEvent(EventBundle events) throws ClientException {

            for (Event event : events.getEvents()) {
                handleEvent(event);
            }
        }

        public void handleEvent(Event event) throws ClientException {
            if (DocumentEventTypes.ABOUT_TO_REMOVE.equals(event.getName())) {
                log.debug("CommentEventListener processing ABOUT_TO_REMOVE event");
                EventContext ctx = event.getContext();
                if (ctx instanceof DocumentEventContext) {
                    DocumentEventContext docCtx = (DocumentEventContext) ctx;
                    DocumentModel doc = docCtx.getSourceDocument();
                    CoreSession coreSession = docCtx.getCoreSession();
                    CommentServiceConfig config = CommentServiceHelper.getCommentService().getConfig();
                    try {
                        RelationManager relationManager = Framework.getService(RelationManager.class);
                        doProcess(coreSession, relationManager, config, doc);
                        log.debug("CommentEventListener processed ABOUT_TO_REMOVE successfully");
                    }
                    catch (Exception e) {
                        log.error("Error during message processing", e);
                    }

                    log.debug("CommentEventListener exiting");
                    return;
                }
            }
        }


        protected abstract void doProcess(CoreSession coreSession, RelationManager relationManager, CommentServiceConfig config, DocumentModel docMessage) throws Exception;

}
