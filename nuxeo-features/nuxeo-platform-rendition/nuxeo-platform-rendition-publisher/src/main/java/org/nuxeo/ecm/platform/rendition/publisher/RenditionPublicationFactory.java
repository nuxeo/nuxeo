/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.rendition.publisher;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;
import org.nuxeo.ecm.platform.publisher.api.PublisherException;
import org.nuxeo.ecm.platform.publisher.jbpm.CoreProxyWithWorkflowFactory;
import org.nuxeo.ecm.platform.rendition.RenditionException;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.runtime.api.Framework;

/**
 * Implementation of {@link PublishedDocumentFactory} that uses the
 * {@link RenditionService} to publish a Rendition of the given document.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.4.1
 */
public class RenditionPublicationFactory extends CoreProxyWithWorkflowFactory
        implements PublishedDocumentFactory {

    public static final String RENDITION_NAME_PARAMETER_KEY = "renditionName";

    protected RenditionService renditionService;

    @Override
    public PublishedDocument publishDocument(DocumentModel doc,
            PublicationNode targetNode, Map<String, String> params)
            throws ClientException {
        if (params != null && params.containsKey(RENDITION_NAME_PARAMETER_KEY)) {
            String renditionName = params.get(RENDITION_NAME_PARAMETER_KEY);
            if (!StringUtils.isEmpty(renditionName)) {
                DocumentRef renditionDocumentRef;
                try {
                    renditionDocumentRef = getRenditionService().render(doc,
                            renditionName);
                } catch (RenditionException e) {
                    throw new PublisherException(e.getLocalizedMessage(), e);
                }
                DocumentModel renditionDocument = coreSession.getDocument(renditionDocumentRef);
                PublishedDocument publishedDocument = super.publishDocument(
                        renditionDocument, targetNode, params);

                new RemoveACP(coreSession, renditionDocumentRef).runUnrestricted();

                return publishedDocument;
            }
        }
        return super.publishDocument(doc, targetNode, params);
    }

    protected RenditionService getRenditionService() throws ClientException {
        if (renditionService == null) {
            try {
                renditionService = Framework.getService(RenditionService.class);
            } catch (Exception e) {
                final String errMsg = "Error connecting to RenditionService. "
                        + e.getMessage();
                throw new ClientException(errMsg, e);
            }
            if (renditionService == null) {
                throw new ClientException("RenditionService service not bound");
            }
        }
        return renditionService;
    }

    public static class RemoveACP extends UnrestrictedSessionRunner {

        protected DocumentRef docRef;

        public RemoveACP(CoreSession session, DocumentRef docRef) {
            super(session);
            this.docRef = docRef;
        }

        @Override
        public void run() throws ClientException {
            ACP acp = new ACPImpl();
            session.setACP(docRef, acp, true);
        }

    }

}
