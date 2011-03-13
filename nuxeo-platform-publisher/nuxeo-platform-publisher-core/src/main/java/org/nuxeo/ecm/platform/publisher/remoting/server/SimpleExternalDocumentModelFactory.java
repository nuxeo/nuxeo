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

package org.nuxeo.ecm.platform.publisher.remoting.server;

import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.platform.publisher.api.AbstractBasePublishedDocumentFactory;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;
import org.nuxeo.ecm.platform.publisher.impl.core.SimpleCorePublishedDocument;
import org.nuxeo.ecm.platform.versioning.api.VersioningActions;
import org.nuxeo.runtime.api.Framework;

import java.util.Map;

/**
 * {@link PublishedDocumentFactory} implementation that creates
 * {@link DocumentModel} instead of simple proxies.
 *
 * @author tiry
 */
public class SimpleExternalDocumentModelFactory extends
        AbstractBasePublishedDocumentFactory implements
        PublishedDocumentFactory {

    public PublishedDocument publishDocument(DocumentModel doc,
            PublicationNode targetNode, Map<String, String> params)
            throws ClientException {

        PathSegmentService pss;
        try {
            pss = Framework.getService(PathSegmentService.class);
        } catch (Exception e) {
            throw new ClientException(e);
        }
        doc.setPathInfo(targetNode.getPath(), "remote_doc_" + pss.generatePathSegment(doc));
        // We don't want to erase the current version
        final ScopedMap ctxData = doc.getContextData();
        ctxData.putScopedValue(ScopeType.REQUEST,
                VersioningActions.SKIP_VERSIONING, true);
        doc = coreSession.createDocument(doc);
        coreSession.save();

        return new ExternalCorePublishedDocument(doc);
    }

    @Override
    protected boolean needToVersionDocument(DocumentModel doc) {
        if (!doc.getRepositoryName().equalsIgnoreCase(
                coreSession.getRepositoryName())) {
            return false;
        } else {
            return super.needToVersionDocument(doc);
        }
    }

    /*
     * public DocumentModel unwrapPublishedDocument(PublishedDocument pubDoc)
     * throws ClientException { if (pubDoc instanceof
     * SimpleCorePublishedDocument) { SimpleCorePublishedDocument pubProxy =
     * (SimpleCorePublishedDocument) pubDoc; return pubProxy.getProxy(); } if
     * (pubDoc instanceof ExternalCorePublishedDocument) {
     * ExternalCorePublishedDocument pubExt = (ExternalCorePublishedDocument)
     * pubDoc; return pubExt.getDocumentModel(); } throw new ClientException(
     * "factory can not unwrap this PublishedDocument"); }
     */

    public PublishedDocument wrapDocumentModel(DocumentModel doc)
            throws ClientException {
        if (doc.isProxy()) {
            return new SimpleCorePublishedDocument(doc);
        }
        return new ExternalCorePublishedDocument(doc);
    }
}
