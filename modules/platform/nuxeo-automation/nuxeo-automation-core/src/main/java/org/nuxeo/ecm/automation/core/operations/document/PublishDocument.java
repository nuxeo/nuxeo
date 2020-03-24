/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.operations.document;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = PublishDocument.ID, category = Constants.CAT_DOCUMENT, label = "Publish Document", description = "Publish the input document into the target section. Existing proxy is overrided if the override attribute is set. Return the created proxy.", aliases = { "Document.Publish" })
public class PublishDocument {

    public static final String ID = "Document.PublishToSection";

    @Context
    protected CoreSession session;

    @Param(name = "target")
    protected DocumentModel target;

    @Param(name = "override", required = false, values = "true")
    protected boolean override = true;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) {
        DocumentModel proxy = session.publishDocument(doc, target, override);
        notifyPublishedEvent(doc);
        notifyPublishedEvent(proxy);
        return proxy;
    }

    /**
     * @since 10.3
     */
    protected void notifyPublishedEvent(DocumentModel doc) {
        Map<String, Serializable> properties = new HashMap<>();
        properties.put(CoreEventConstants.REPOSITORY_NAME, doc.getRepositoryName());
        properties.put(CoreEventConstants.DOC_LIFE_CYCLE, doc.getCurrentLifeCycleState());

        DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), doc);
        ctx.setProperties(properties);
        ctx.setCategory(DocumentEventCategories.EVENT_DOCUMENT_CATEGORY);

        Event event = ctx.newEvent(DocumentEventTypes.DOCUMENT_PUBLISHED);
        Framework.getService(EventProducer.class).fireEvent(event);
    }

}
