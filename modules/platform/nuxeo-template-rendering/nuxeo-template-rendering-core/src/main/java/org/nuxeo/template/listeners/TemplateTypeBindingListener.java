/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thierry Delprat
 */
package org.nuxeo.template.listeners;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitFilteringEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.ShallowDocumentModel;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.adapters.TemplateAdapterFactory;
import org.nuxeo.template.api.TemplateProcessorService;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

public class TemplateTypeBindingListener implements PostCommitFilteringEventListener {

    protected static Log log = LogFactory.getLog(TemplateTypeBindingListener.class);

    @Override
    public boolean acceptEvent(Event event) {
        EventContext context = event.getContext();
        if (!(context instanceof DocumentEventContext)) {
            return false;
        }
        DocumentModel doc = ((DocumentEventContext) context).getSourceDocument();
        if (doc == null || doc.isVersion()) {
            return false;
        }
        // we cannot directly adapt the ShallowDocumentModel,
        // so check the adapter factory manually
        return TemplateAdapterFactory.isAdaptable(doc, TemplateSourceDocument.class);
    }

    @Override
    public void handleEvent(EventBundle eventBundle) {
        if (eventBundle.containsEventName(DOCUMENT_CREATED) || eventBundle.containsEventName(DOCUMENT_UPDATED)) {

            TemplateProcessorService tps = Framework.getService(TemplateProcessorService.class);

            for (Event event : eventBundle) {
                if (DOCUMENT_CREATED.equals(event.getName()) || DOCUMENT_UPDATED.equals(event.getName())) {
                    EventContext ctx = event.getContext();
                    if (ctx instanceof DocumentEventContext) {
                        DocumentEventContext docCtx = (DocumentEventContext) ctx;
                        DocumentModel targetDoc = docCtx.getSourceDocument();

                        if (targetDoc.isVersion()) {
                            continue;
                        }
                        if (targetDoc instanceof ShallowDocumentModel) {
                            log.warn("Skip unconnected document with type " + targetDoc.getType() + " and path "
                                    + targetDoc.getPathAsString());
                            continue;
                        }
                        TemplateSourceDocument tmpl = targetDoc.getAdapter(TemplateSourceDocument.class);
                        if (tmpl != null) {
                            tps.registerTypeMapping(targetDoc);
                            // be sure to trigger invalidations in unit tests
                            targetDoc.getCoreSession().save();
                        }
                    }
                }
            }
        }
    }

}
