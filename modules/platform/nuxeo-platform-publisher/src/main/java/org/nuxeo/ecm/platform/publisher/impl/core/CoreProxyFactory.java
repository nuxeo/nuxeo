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

package org.nuxeo.ecm.platform.publisher.impl.core;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.publisher.api.AbstractBasePublishedDocumentFactory;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;
import org.nuxeo.ecm.platform.publisher.api.PublishingEvent;

import java.util.Map;

/**
 * Implementation of the {@link PublishedDocumentFactory} for simple core implementation using native proxy system.
 *
 * @author tiry
 */
public class CoreProxyFactory extends AbstractBasePublishedDocumentFactory implements PublishedDocumentFactory {

    @Override
    public PublishedDocument publishDocument(DocumentModel doc, PublicationNode targetNode, Map<String, String> params)
            {

        DocumentModel targetDocModel;
        if (targetNode instanceof CoreFolderPublicationNode) {
            CoreFolderPublicationNode coreNode = (CoreFolderPublicationNode) targetNode;
            targetDocModel = coreNode.getTargetDocumentModel();
        } else {
            targetDocModel = coreSession.getDocument(new PathRef(targetNode.getPath()));
        }

        DocumentModel proxy;
        if ((params != null) && (params.containsKey("overwriteExistingProxy"))) {
            proxy = coreSession.publishDocument(doc, targetDocModel,
                    Boolean.parseBoolean(params.get("overwriteExistingProxy")));
        } else {
            proxy = coreSession.publishDocument(doc, targetDocModel);
        }
        if ((params != null) && ("true".equalsIgnoreCase(params.get("batchSave")))) {
            // no save
        } else {
            coreSession.save();
        }
        notifyEvent(PublishingEvent.documentPublished, proxy, coreSession);
        return new SimpleCorePublishedDocument(proxy);
    }

    @Override
    public DocumentModel snapshotDocumentBeforePublish(DocumentModel doc) {
        // snapshoting is done as part of the publishing
        return doc;
    }

    public DocumentModel unwrapPublishedDocument(PublishedDocument pubDoc) {
        if (pubDoc instanceof SimpleCorePublishedDocument) {
            SimpleCorePublishedDocument pubProxy = (SimpleCorePublishedDocument) pubDoc;
            return pubProxy.getProxy();
        }
        throw new NuxeoException("factory can not unwrap this PublishedDocument");
    }

    @Override
    public PublishedDocument wrapDocumentModel(DocumentModel doc) {
        return new SimpleCorePublishedDocument(doc);
    }

}
