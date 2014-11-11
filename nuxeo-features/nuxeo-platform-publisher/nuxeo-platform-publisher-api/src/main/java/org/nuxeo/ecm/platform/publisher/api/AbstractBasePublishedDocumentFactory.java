/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.publisher.api;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.publisher.rules.PublishingValidatorException;
import org.nuxeo.ecm.platform.publisher.rules.ValidatorsRule;
import org.nuxeo.runtime.api.Framework;

public abstract class AbstractBasePublishedDocumentFactory implements
        PublishedDocumentFactory {

    public static final String ENABLE_SNAPSHOT = "enableSnapshot";

    public static final String TARGET_PUBLISHED_DOCUMENT_STATE = "targetPublishedDocumentState";

    protected CoreSession coreSession;

    protected Map<String, String> parameters;

    protected PublicationTree publicationTree;

    protected ValidatorsRule validatorsRule;

    protected EventProducer eventProducer;

    public void init(CoreSession coreSession, ValidatorsRule validatorsRule, Map<String, String> parameters)
            throws ClientException {
        this.coreSession = coreSession;
        this.parameters = parameters;
        this.validatorsRule = validatorsRule;
        if (this.parameters == null) {
            this.parameters = new HashMap<String, String>();
        }
    }

    public void init(CoreSession coreSession, Map<String, String> parameters)
            throws ClientException {
        init(coreSession, null, parameters);
    }

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

    public PublishedDocument publishDocument(DocumentModel doc,
            PublicationNode targetNode) throws ClientException {
        return publishDocument(doc, targetNode, null);
    }

    protected boolean needToVersionDocument(DocumentModel doc) {
        if (!doc.isVersion() && doc.isVersionable()) {
            return true;
        }
        return false;
    }

    public DocumentModel snapshotDocumentBeforePublish(DocumentModel doc)
            throws ClientException {

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

    public String[] getValidatorsFor(DocumentModel dm)
            throws PublishingValidatorException {
        return validatorsRule.computesValidatorsFor(dm);
    }

    public ValidatorsRule getValidatorsRule()
            throws PublishingValidatorException {
        return validatorsRule;
    }

    public void validatorPublishDocument(PublishedDocument publishedDocument,
            String comment) throws ClientException {
    }

    public void validatorRejectPublication(PublishedDocument publishedDocument,
            String comment) throws ClientException {
    }

    public boolean canManagePublishing(PublishedDocument publishedDocument) throws ClientException {
        return false;
    }

    public boolean hasValidationTask(PublishedDocument publishedDocument) throws ClientException {
        return false;
    }

    /*
     * -------- Event firing --------
     */

    protected void notifyEvent(PublishingEvent event, DocumentModel doc,
            CoreSession coreSession) throws PublishingException {
        try {
            notifyEvent(event.name(), null, null, null, doc, coreSession);
        } catch (ClientException e) {
            throw new PublishingException(e);
        }
    }

    protected void notifyEvent(String eventId,
            Map<String, Serializable> properties, String comment,
            String category, DocumentModel dm, CoreSession coreSession)
            throws ClientException {
        // Default category
        if (category == null) {
            category = DocumentEventCategories.EVENT_DOCUMENT_CATEGORY;
        }
        if (properties == null) {
            properties = new HashMap<String, Serializable>();
        }
        properties.put(CoreEventConstants.REPOSITORY_NAME,
                dm.getRepositoryName());
        properties.put(CoreEventConstants.SESSION_ID,
                coreSession.getSessionId());
        properties.put(CoreEventConstants.DOC_LIFE_CYCLE,
                dm.getCurrentLifeCycleState());

        DocumentEventContext ctx = new DocumentEventContext(coreSession,
                coreSession.getPrincipal(), dm);
        ctx.setProperties(properties);
        ctx.setComment(comment);
        ctx.setCategory(category);

        Event event = ctx.newEvent(eventId);
        getEventProducer().fireEvent(event);
    }

    protected EventProducer getEventProducer() throws ClientException {
        if (eventProducer == null) {
            try {
                eventProducer = Framework.getService(EventProducer.class);
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }
        return eventProducer;
    }

}
