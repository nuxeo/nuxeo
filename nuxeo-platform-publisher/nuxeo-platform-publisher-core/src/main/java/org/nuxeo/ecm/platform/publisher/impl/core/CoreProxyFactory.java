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

package org.nuxeo.ecm.platform.publisher.impl.core;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.publisher.api.AbstractBasePublishedDocumentFactory;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;
import org.nuxeo.ecm.platform.publisher.api.PublishingEvent;

import java.util.Map;

/**
 * Implementation of the {@link PublishedDocumentFactory} for simple core
 * implementation using native proxy system.
 *
 * @author tiry
 */
public class CoreProxyFactory extends AbstractBasePublishedDocumentFactory
        implements PublishedDocumentFactory {

    public PublishedDocument publishDocument(DocumentModel doc,
            PublicationNode targetNode, Map<String, String> params)
            throws ClientException {

        DocumentModel targetDocModel;
        if (targetNode instanceof CoreFolderPublicationNode) {
            CoreFolderPublicationNode coreNode = (CoreFolderPublicationNode) targetNode;
            targetDocModel = coreNode.getTargetDocumentModel();
        } else {
            targetDocModel = coreSession.getDocument(new PathRef(
                    targetNode.getPath()));
        }

        DocumentModel proxy ;
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
        notifyEvent(PublishingEvent.documentPublished, proxy,
                            coreSession);
        return new SimpleCorePublishedDocument(proxy);
    }

    public DocumentModel snapshotDocumentBeforePublish(DocumentModel doc) {
        // snapshoting is done as part of the publishing
        return doc;
    }

    public DocumentModel unwrapPublishedDocument(PublishedDocument pubDoc)
            throws ClientException {
        if (pubDoc instanceof SimpleCorePublishedDocument) {
            SimpleCorePublishedDocument pubProxy = (SimpleCorePublishedDocument) pubDoc;
            return pubProxy.getProxy();
        }
        throw new ClientException(
                "factory can not unwrap this PublishedDocument");
    }

    public PublishedDocument wrapDocumentModel(DocumentModel doc)
            throws ClientException {
        return new SimpleCorePublishedDocument(doc);
    }

}
