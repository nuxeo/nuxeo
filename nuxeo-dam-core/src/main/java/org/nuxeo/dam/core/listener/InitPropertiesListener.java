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
 *     Nuxeo
 */

package org.nuxeo.dam.core.listener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.dam.core.Constants;
import org.nuxeo.dam.core.service.InheritedPropertiesService;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

public class InitPropertiesListener implements EventListener {

    /**
     * Used to find the first parent accessible by a user, ie. the first parent
     * where the user has the READ permission.
     */
    protected static class AccessibleParentFinder extends UnrestrictedSessionRunner {

        protected CoreSession userSession;

        public DocumentModel doc = null;
        public DocumentModel parent = null;

        public AccessibleParentFinder(CoreSession session, DocumentModel doc) {
            super(session);
            this.userSession = session;
            this.doc = doc;
        }

        @Override
        public void run() throws ClientException {
            parent = getFirstParentAccessibleByUser(doc);
        }

        protected DocumentModel getFirstParentAccessibleByUser(DocumentModel doc) throws ClientException {
            DocumentModel parent = session.getDocument(doc.getParentRef());
            if (parent == null || "/".equals(parent.getPathAsString())) {
                return null;
            }

            if (userSession.hasPermission(parent.getRef(), SecurityConstants.READ)) {
                return parent;
            } else {
                return getFirstParentAccessibleByUser(parent);
            }
        }

    }

    private static final Log log = LogFactory.getLog(InitPropertiesListener.class);

    protected InheritedPropertiesService inheritedPropertiesService;

    public void handleEvent(Event event) throws ClientException {
        EventContext ctx = event.getContext();

        if (ctx instanceof DocumentEventContext) {
            DocumentEventContext docCtx = (DocumentEventContext) ctx;
            DocumentModel doc = docCtx.getSourceDocument();
            CoreSession coreSession = docCtx.getCoreSession();

            if (doc.hasFacet(Constants.ASSET_FACET)
                    && !Constants.IMPORT_SET_TYPE.equals(doc.getType())) {

                DocumentModel importSet = getImportSet(coreSession, doc);
                if (importSet == null || "/".equals(importSet.getPathAsString())) {
                    // there is no or no accessible importset parent, don't update
                    // the document.
                    return;
                }

                InheritedPropertiesService service = getInheritedPropertiesService();
                if (service != null) {
                    service.inheritProperties(importSet, doc);
                }
            }
        }
    }

    /**
     * Returns the first {@code ImportSet} parent, or {@code null} if no parent
     * is accessible.
     *
     * @param doc
     */
    protected DocumentModel getImportSet(CoreSession session, DocumentModel doc) throws ClientException {
        if (Constants.IMPORT_SET_TYPE.equals(doc.getType())) {
            return doc;
        } else {
            DocumentModel parent = getFirstAccessibleParent(session, doc);
            if (parent == null || "/".equals(parent.getPathAsString())) {
                return null;
            } else {
                return getImportSet(session, parent);
            }
        }
    }

    protected DocumentModel getFirstAccessibleParent(CoreSession session, DocumentModel doc) throws ClientException {
        AccessibleParentFinder finder = new AccessibleParentFinder(session, doc);
        finder.runUnrestricted();
        return finder.parent;
    }

    protected InheritedPropertiesService getInheritedPropertiesService() {
        if (inheritedPropertiesService == null) {
            try {
                inheritedPropertiesService = Framework.getService(InheritedPropertiesService.class);
            } catch (Exception e) {
                log.error("Unable to retrieve InheritedPropertiesService", e);
            }
        }
        return inheritedPropertiesService;
    }

}
