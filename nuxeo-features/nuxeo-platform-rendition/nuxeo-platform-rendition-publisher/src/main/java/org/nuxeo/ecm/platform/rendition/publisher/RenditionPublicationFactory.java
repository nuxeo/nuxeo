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

import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_FACET;

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
import org.nuxeo.ecm.platform.publisher.api.PublishingException;
import org.nuxeo.ecm.platform.publisher.impl.core.SimpleCorePublishedDocument;
import org.nuxeo.ecm.platform.publisher.task.CoreProxyWithWorkflowFactory;
import org.nuxeo.ecm.platform.rendition.Rendition;
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
                DocumentModel renditionDocument = null;
                Rendition rendition = null;
                try {
                    rendition = getRenditionService().getRendition(doc,
                            renditionName, true);
                    if (rendition != null) {
                        renditionDocument = rendition.getHostDocument();
                    } else {
                        throw new PublisherException(
                                "Unable to render the document");
                    }
                } catch (RenditionException e) {
                    throw new PublisherException(e.getLocalizedMessage(), e);
                }
                PublishedDocument publishedDocument = super.publishDocument(
                        renditionDocument, targetNode, params);
                DocumentModel proxy = ((SimpleCorePublishedDocument) publishedDocument).getProxy();
                proxy.attach(doc.getSessionId());
                if (!hasValidationTask(publishedDocument)) {
                    removeExistingProxiesOnPreviousVersions(proxy);
                }
                return publishedDocument;
            }

        }
        return super.publishDocument(doc, targetNode, params);
    }

    @Override
    protected DocumentModel getLiveDocument(CoreSession session,
            DocumentModel proxy) throws ClientException {
        if (!proxy.hasFacet(RENDITION_FACET)) {
            return super.getLiveDocument(session, proxy);
        }
        RenditionLiveDocFetcher fetcher = new RenditionLiveDocFetcher(session,
                proxy);
        fetcher.runUnrestricted();
        return fetcher.getLiveDocument();
    }

    @Override
    protected void removeExistingProxiesOnPreviousVersions(
            DocumentModel newProxy) throws PublishingException {

        if (!newProxy.hasFacet(RENDITION_FACET)) {
            super.removeExistingProxiesOnPreviousVersions(newProxy);
            return;
        }
        RenditionsRemover remover = new RenditionsRemover(newProxy);
        try {
            remover.runUnrestricted();
        } catch (ClientException e) {
            throw new PublishingException(
                    "Unable to remove old puiblished renditions", e);
        }

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
