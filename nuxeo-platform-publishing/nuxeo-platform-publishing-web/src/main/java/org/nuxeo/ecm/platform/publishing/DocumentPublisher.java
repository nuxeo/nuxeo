/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     alexandre
 */
package org.nuxeo.ecm.platform.publishing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.LogFactory;
import org.jboss.seam.core.Events;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.publishing.api.PublishingService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.apache.commons.logging.Log;

/**
 * @author alexandre
 *
 */
public class DocumentPublisher extends UnrestrictedSessionRunner {
    protected final CoreSession coreSession;

    protected final String comment;

    protected UserManager userManager;

    protected PublishingService publishingService;

    protected final boolean setIssuedDate;

    protected final DocumentRef docRef;

    protected final DocumentRef sectionRef;

    private static final Log log = LogFactory.getLog(DocumentPublisher.class);

    /** Returned proxy. */
    public DocumentRef proxyRef;

    protected DocumentPublisher(CoreSession coreSession, DocumentModel doc,
            DocumentModel section, String comment) throws ClientException {
        super(coreSession);
        this.coreSession = coreSession;
        this.comment = comment;
        docRef = doc.getRef();
        sectionRef = section.getRef();
        setIssuedDate = coreSession.isDirty(docRef) && !doc.isProxy();
        try {
            this.userManager = Framework.getService(UserManager.class);
            this.publishingService = Framework.getService(PublishingService.class);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Publishing service is not deployed.", e);
        }
    }

    @Override
    public void run() throws ClientException {
        DocumentModel doc = session.getDocument(docRef);
        DocumentModel section = session.getDocument(sectionRef);
        if (setIssuedDate) {
            doc.setProperty("dublincore", "issued", Calendar.getInstance());
            // make sure that saveDocument doesn't create a snapshot,
            // as publishDocument will do it
            doc.putContextData(org.nuxeo.common.collections.ScopeType.REQUEST,
                    VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY,
                    Boolean.FALSE);
            session.saveDocument(doc);
        }
        DocumentModel proxy = session.publishDocument(doc, section);
        session.save();
        proxyRef = proxy.getRef();

        // notify event
        Map<String, Serializable> eventInfo = new HashMap<String, Serializable>();
        eventInfo.put("proxy", proxy);
        eventInfo.put("targetSection", section.getName());
        eventInfo.put("sectionPath", section.getPathAsString());
        String eventId;
        if (isUnrestricted) {
            // submitted through workflow, this event starts the workflow
            eventId = org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_SUBMITED_FOR_PUBLICATION;
            // additional event info: recipients
            String[] validators = publishingService.getValidatorsFor(proxy);
            List<String> recipients = new ArrayList<String>(validators.length);
            for (String user : validators) {
                recipients.add((userManager.getGroup(user) == null ? "user:"
                        : "group:")
                        + user);
            }
            eventInfo.put("recipients", StringUtils.join(recipients, '|'));
        } else {
            // direct publishing
            eventId = org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_PUBLISHED;
        }
        notifyEvent(eventId, eventInfo, comment, null, doc);
        if (isUnrestricted) {
            /*
             * Invalidate dashboard items using Seam since a publishing workflow
             * might have been started. XXX We need to do it here since the
             * workflow starts in a message driven bean in a async way. Not sure
             * we can optimize right now.
             */
            Events.instance().raiseEvent("WORKFLOW_NEW_STARTED");
        }
    }

    public void notifyEvent(String eventId,
            Map<String, Serializable> properties, String comment,
            String category, DocumentModel dm) throws ClientException {

        // Default category
        if (category == null) {
            category = DocumentEventCategories.EVENT_DOCUMENT_CATEGORY;
        }

        if (properties == null) {
            properties = new HashMap<String, Serializable>();
        }

        properties.put(CoreEventConstants.REPOSITORY_NAME,
                coreSession.getRepositoryName());
        properties.put(CoreEventConstants.SESSION_ID,
                coreSession.getSessionId());
        properties.put(CoreEventConstants.DOC_LIFE_CYCLE,
                dm.getCurrentLifeCycleState());

        DocumentEventContext ctx = new DocumentEventContext(coreSession,coreSession.getPrincipal(),dm);
        ctx.setProperties(properties);
        ctx.setComment(comment);
        ctx.setCategory(category);

        Event event = ctx.newEvent(eventId);

        EventProducer evtProducer = null;

        try {
            evtProducer = Framework.getService(EventProducer.class);
        } catch (Exception e) {
            log.error("Unable to get EventProducer", e);
        }

        try {
            evtProducer.fireEvent(event);
        } catch (Exception e) {
            log.error("Unable to send message", e);
        }
    }
}
