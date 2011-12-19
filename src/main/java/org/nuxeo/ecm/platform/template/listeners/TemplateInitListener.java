/*
 * (C) Copyright 2006-20012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.template.listeners;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.template.TemplateInput;
import org.nuxeo.ecm.platform.template.adapters.source.TemplateSourceDocument;

/**
 * Listener -- not activtated form now.
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public class TemplateInitListener implements EventListener {

    private static final Log log = LogFactory.getLog(TemplateInitListener.class);

    public void handleEvent(Event event) throws ClientException {

        EventContext ctx = event.getContext();

        if (DOCUMENT_CREATED.equals(event.getName()) || DOCUMENT_UPDATED.equals(event.getName()) )
        {
            if (ctx instanceof DocumentEventContext) {
                DocumentEventContext docCtx = (DocumentEventContext) ctx;

                DocumentModel targetDoc = docCtx.getSourceDocument();

                if (targetDoc.isVersion()) {
                    return ;
                }

                TemplateSourceDocument templateDoc = targetDoc.getAdapter(TemplateSourceDocument.class);
                if (templateDoc==null) {
                    return;
                }
                List<TemplateInput> params = templateDoc.getParams();
                if (params==null || params.size()==0) {
                    try {
                        templateDoc.initParamsFromFile(false);
                    } catch (Exception e) {
                        log.error("Error during parameter automatic initialization", e);
                    }
                }
            }
        }
    }
}
