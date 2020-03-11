/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */

package org.nuxeo.apidoc.listener;

import static org.nuxeo.apidoc.listener.AttributesExtractorStater.DOC_TYPES;

import org.nuxeo.apidoc.worker.ExtractXmlAttributesWorker;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Listener that trigger a Worker to extract XML Attributes and store them in a
 * property that will be indexed.
 *
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 8.3
 */
public class AttributesExtractorScheduler implements EventListener {

    public static final String EXTRACT_XML_ATTRIBUTES_NEEDED = "extractXmlAttributesNeeded";

    @Override
    public void handleEvent(Event event) {
        if (!(event.getContext() instanceof DocumentEventContext)) {
            return;
        }

        DocumentEventContext ctx = (DocumentEventContext) event.getContext();
        DocumentModel doc = ctx.getSourceDocument();
        if (!DOC_TYPES.contains(doc.getType())) {
            return;
        }

        Boolean flag = (Boolean) ctx.getProperty(EXTRACT_XML_ATTRIBUTES_NEEDED);
        if (!Boolean.TRUE.equals(flag)) {
            return;
        }

        Work worker = new ExtractXmlAttributesWorker(ctx.getRepositoryName(), ctx.getPrincipal().getName(), doc.getId());
        Framework.getService(WorkManager.class).schedule(worker, true);
    }
}
