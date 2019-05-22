/*
 * (C) Copyright 2006-2018 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *     Thierry Delprat (td@nuxeo.com)
 *     Ruslan Spivak (rspivak@nuxeo.com)
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.dublincore.listener;

import static org.nuxeo.ecm.core.api.LifeCycleConstants.TRANSITION_EVENT;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_CREATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED_BY_COPY;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_PUBLISHED;
import static org.nuxeo.ecm.core.schema.FacetNames.SYSTEM_DOCUMENT;
import static org.nuxeo.ecm.platform.dublincore.constants.DublinCoreConstants.DUBLINCORE_CONTRIBUTORS_PROPERTY;
import static org.nuxeo.ecm.platform.dublincore.constants.DublinCoreConstants.DUBLINCORE_CREATOR_PROPERTY;
import static org.nuxeo.ecm.platform.dublincore.constants.DublinCoreConstants.DUBLINCORE_LAST_CONTRIBUTOR_PROPERTY;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.dublincore.service.DublinCoreStorageService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Core Event Listener for updating DublinCore.
 */
public class DublinCoreListener implements EventListener {

    private static final Log log = LogFactory.getLog(DublinCoreListener.class);

    public static final String DISABLE_DUBLINCORE_LISTENER = "disableDublinCoreListener";

    private static final String RESET_CREATOR_PROPERTY = "nuxeo.dclistener.reset-creator-on-copy";

    /**
     * Core event notification.
     * <p>
     * Gets core events and updates DublinCore if needed.
     *
     * @param event event fired at core layer
     */
    @Override
    public void handleEvent(Event event) {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }

        Boolean block = (Boolean) event.getContext().getProperty(DISABLE_DUBLINCORE_LISTENER);
        if (Boolean.TRUE.equals(block)) {
            // ignore the event - we are blocked by the caller
            return;
        }

        DocumentEventContext docCtx = (DocumentEventContext) ctx;
        DocumentModel doc = docCtx.getSourceDocument();

        if (doc.isVersion()) {
            log.debug("No DublinCore update on versions except for the issued date");
            return;
        }

        if (doc.hasFacet(SYSTEM_DOCUMENT)) {
            // ignore the event for System documents
            return;
        }

        Date eventDate = new Date(event.getTime());
        Calendar cEventDate = Calendar.getInstance();
        cEventDate.setTime(eventDate);

        String eventId = event.getName();

        if (doc.isProxy()) {
            if (eventId.equals(DOCUMENT_PUBLISHED)) {
                CoreSession session = event.getContext().getCoreSession();
                UnrestrictedPropertySetter setter = new UnrestrictedPropertySetter(session, doc.getRef(), "dc:issued",
                        cEventDate);
                setter.runUnrestricted();
            }
            if (doc.isImmutable()) {
                // proxies with attached schemas can be changed
                // (and therefore saved), but they're still mostly
                // immutable, so don't attempt to set modification dates
                // on them
                return;
            }
            // live proxies may be updated normally, except at creation time (don't update the live doc)
            if (eventId.equals(ABOUT_TO_CREATE)) {
                return;
            }
        }

        DublinCoreStorageService service = Framework.getService(DublinCoreStorageService.class);

        Boolean resetCreator = (Boolean) event.getContext().getProperty(CoreEventConstants.RESET_CREATOR);
        boolean resetCreatorProperty = Framework.getService(ConfigurationService.class)
                                                .isBooleanTrue(RESET_CREATOR_PROPERTY);
        Boolean dirty = (Boolean) event.getContext().getProperty(CoreEventConstants.DOCUMENT_DIRTY);
        if ((eventId.equals(BEFORE_DOC_UPDATE) && Boolean.TRUE.equals(dirty))
                || (eventId.equals(TRANSITION_EVENT) && !doc.isImmutable())) {
            service.setModificationDate(doc, cEventDate);
            service.addContributor(doc, event);
        } else if (eventId.equals(ABOUT_TO_CREATE)) {
            service.setCreationDate(doc, cEventDate);
            service.setModificationDate(doc, cEventDate);
            service.addContributor(doc, event);
        } else if (eventId.equals(DOCUMENT_CREATED_BY_COPY)
                && (resetCreatorProperty || Boolean.TRUE.equals(resetCreator))) {
            Framework.doPrivileged(() -> {
                doc.setPropertyValue(DUBLINCORE_CREATOR_PROPERTY, null);
                doc.setPropertyValue(DUBLINCORE_CONTRIBUTORS_PROPERTY, null);
                doc.setPropertyValue(DUBLINCORE_LAST_CONTRIBUTOR_PROPERTY, null);
            });
            service.setCreationDate(doc, cEventDate);
            service.setModificationDate(doc, cEventDate);
            service.addContributor(doc, event);
        }
    }

    protected class UnrestrictedPropertySetter extends UnrestrictedSessionRunner {

        DocumentRef docRef;

        String xpath;

        Serializable value;

        protected UnrestrictedPropertySetter(CoreSession session, DocumentRef docRef, String xpath,
                Serializable value) {
            super(session);
            this.docRef = docRef;
            this.xpath = xpath;
            this.value = value;
        }

        @Override
        public void run() {
            DocumentModel doc = session.getSourceDocument(docRef);
            if (doc != null) {
                doc.setPropertyValue(xpath, value);
                session.saveDocument(doc);
            }

        }

    }

}
