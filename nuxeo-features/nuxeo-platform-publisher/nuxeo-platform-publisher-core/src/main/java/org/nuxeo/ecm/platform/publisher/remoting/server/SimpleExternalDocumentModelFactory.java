/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.publisher.remoting.server;

import java.util.Map;

import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.platform.publisher.api.AbstractBasePublishedDocumentFactory;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;
import org.nuxeo.ecm.platform.publisher.impl.core.SimpleCorePublishedDocument;
import org.nuxeo.runtime.api.Framework;

/**
 * {@link PublishedDocumentFactory} implementation that creates {@link DocumentModel} instead of simple proxies.
 *
 * @author tiry
 */
public class SimpleExternalDocumentModelFactory extends AbstractBasePublishedDocumentFactory
        implements PublishedDocumentFactory {

    public PublishedDocument publishDocument(DocumentModel doc, PublicationNode targetNode,
            Map<String, String> params) {

        PathSegmentService pss = Framework.getService(PathSegmentService.class);
        doc.setPathInfo(targetNode.getPath(), "remote_doc_" + pss.generatePathSegment(doc));
        // We don't want to erase the current version
        final ScopedMap ctxData = doc.getContextData();
        ctxData.putScopedValue(ScopeType.REQUEST, VersioningService.SKIP_VERSIONING, true);
        doc = coreSession.createDocument(doc);
        coreSession.save();

        return new ExternalCorePublishedDocument(doc);
    }

    @Override
    protected boolean needToVersionDocument(DocumentModel doc) {
        if (!doc.getRepositoryName().equalsIgnoreCase(coreSession.getRepositoryName())) {
            return false;
        } else {
            return super.needToVersionDocument(doc);
        }
    }

    public PublishedDocument wrapDocumentModel(DocumentModel doc) {
        if (doc.isProxy()) {
            return new SimpleCorePublishedDocument(doc);
        }
        return new ExternalCorePublishedDocument(doc);
    }
}
