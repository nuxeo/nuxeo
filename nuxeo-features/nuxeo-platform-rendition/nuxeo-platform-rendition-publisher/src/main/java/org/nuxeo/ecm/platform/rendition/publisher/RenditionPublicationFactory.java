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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;
import org.nuxeo.ecm.platform.publisher.impl.core.SimpleCorePublishedDocument;
import org.nuxeo.ecm.platform.publisher.task.CoreProxyWithWorkflowFactory;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.runtime.api.Framework;

/**
 * Implementation of {@link PublishedDocumentFactory} that uses the {@link RenditionService} to publish a Rendition of
 * the given document.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.4.1
 */
public class RenditionPublicationFactory extends CoreProxyWithWorkflowFactory implements PublishedDocumentFactory {

    public static final String RENDITION_NAME_PARAMETER_KEY = "renditionName";

    @Override
    public PublishedDocument publishDocument(DocumentModel doc, PublicationNode targetNode, Map<String, String> params)
            {
        if (params != null && params.containsKey(RENDITION_NAME_PARAMETER_KEY)) {
            String renditionName = params.get(RENDITION_NAME_PARAMETER_KEY);
            if (!StringUtils.isEmpty(renditionName)) {
                DocumentModel renditionDocument = null;
                Rendition rendition = null;
                rendition = getRenditionService().getRendition(doc, renditionName, true);
                if (rendition != null) {
                    renditionDocument = rendition.getHostDocument();
                } else {
                    throw new NuxeoException("Unable to render the document");
                }
                PublishedDocument publishedDocument = super.publishDocument(renditionDocument, targetNode, params);
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
    protected DocumentModel getLiveDocument(CoreSession session, DocumentModel proxy) {
        if (!proxy.hasFacet(RENDITION_FACET)) {
            return super.getLiveDocument(session, proxy);
        }
        RenditionLiveDocFetcher fetcher = new RenditionLiveDocFetcher(session, proxy);
        fetcher.runUnrestricted();
        return fetcher.getLiveDocument();
    }

    @Override
    protected void removeExistingProxiesOnPreviousVersions(DocumentModel newProxy) {
        if (!newProxy.hasFacet(RENDITION_FACET)) {
            super.removeExistingProxiesOnPreviousVersions(newProxy);
            return;
        }
        RenditionsRemover remover = new RenditionsRemover(newProxy);
        remover.runUnrestricted();
    }

    protected RenditionService getRenditionService() {
        return Framework.getService(RenditionService.class);
    }

    public static class RemoveACP extends UnrestrictedSessionRunner {

        protected DocumentRef docRef;

        public RemoveACP(CoreSession session, DocumentRef docRef) {
            super(session);
            this.docRef = docRef;
        }

        @Override
        public void run() {
            ACP acp = new ACPImpl();
            session.setACP(docRef, acp, true);
        }

    }

}
