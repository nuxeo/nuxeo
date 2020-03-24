/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.publisher.api;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.publisher.rules.ValidatorsRule;
import org.nuxeo.runtime.api.Framework;

public abstract class AbstractBasePublishedDocumentFactory implements PublishedDocumentFactory {

    public static final String ENABLE_SNAPSHOT = "enableSnapshot";

    public static final String TARGET_PUBLISHED_DOCUMENT_STATE = "targetPublishedDocumentState";

    protected CoreSession coreSession;

    protected Map<String, String> parameters;

    protected PublicationTree publicationTree;

    protected ValidatorsRule validatorsRule;

    protected EventProducer eventProducer;

    @Override
    public void init(CoreSession coreSession, ValidatorsRule validatorsRule, Map<String, String> parameters)
            {
        this.coreSession = coreSession;
        this.parameters = parameters;
        this.validatorsRule = validatorsRule;
        if (this.parameters == null) {
            this.parameters = new HashMap<>();
        }
    }

    @Override
    public void init(CoreSession coreSession, Map<String, String> parameters) {
        init(coreSession, null, parameters);
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    protected String getParameter(String name) {
        return parameters.get(name);
    }

    protected boolean isSnapshotingEnabled() {
        String snap = getParameter(ENABLE_SNAPSHOT);
        if (snap == null) {
            return false;
        } else {
            return snap.equalsIgnoreCase("true");
        }
    }

    protected String getTargetPublishedDocumentState() {
        return getParameter(TARGET_PUBLISHED_DOCUMENT_STATE);
    }

    @Override
    public PublishedDocument publishDocument(DocumentModel doc, PublicationNode targetNode) {
        return publishDocument(doc, targetNode, null);
    }

    protected boolean needToVersionDocument(DocumentModel doc) {
        if (!doc.isVersion() && doc.isVersionable()) {
            return true;
        }
        return false;
    }

    @Override
    public DocumentModel snapshotDocumentBeforePublish(DocumentModel doc) {

        if (isSnapshotingEnabled() && needToVersionDocument(doc)) {
            if (doc.isCheckedOut()) {
                doc.checkIn(VersioningOption.MINOR, null);
            }
            coreSession.save();
            List<DocumentModel> versions = coreSession.getVersions(doc.getRef());
            return versions.get(versions.size() - 1);
        } else {
            return doc;
        }
    }

    @Override
    public String[] getValidatorsFor(DocumentModel dm) {
        return validatorsRule.computesValidatorsFor(dm);
    }

    @Override
    public ValidatorsRule getValidatorsRule() {
        return validatorsRule;
    }

    @Override
    public void validatorPublishDocument(PublishedDocument publishedDocument, String comment) {
    }

    @Override
    public void validatorRejectPublication(PublishedDocument publishedDocument, String comment) {
    }

    @Override
    public boolean canManagePublishing(PublishedDocument publishedDocument) {
        return false;
    }

    @Override
    public boolean hasValidationTask(PublishedDocument publishedDocument) {
        return false;
    }

    /*
     * -------- Event firing --------
     */

    protected void notifyEvent(PublishingEvent event, DocumentModel doc, CoreSession coreSession) {
        notifyEvent(event.name(), null, null, null, doc, coreSession);
    }

    protected void notifyEvent(String eventId, Map<String, Serializable> properties, String comment, String category,
            DocumentModel dm, CoreSession coreSession) {
        // Default category
        if (category == null) {
            category = DocumentEventCategories.EVENT_DOCUMENT_CATEGORY;
        }
        if (properties == null) {
            properties = new HashMap<>();
        }
        properties.put(CoreEventConstants.REPOSITORY_NAME, dm.getRepositoryName());
        properties.put(CoreEventConstants.DOC_LIFE_CYCLE, dm.getCurrentLifeCycleState());

        DocumentEventContext ctx = new DocumentEventContext(coreSession, coreSession.getPrincipal(), dm);
        ctx.setProperties(properties);
        ctx.setComment(comment);
        ctx.setCategory(category);

        Event event = ctx.newEvent(eventId);
        getEventProducer().fireEvent(event);
    }

    protected EventProducer getEventProducer() {
        if (eventProducer == null) {
            eventProducer = Framework.getService(EventProducer.class);
        }
        return eventProducer;
    }

}
