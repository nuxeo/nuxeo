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

package org.nuxeo.ecm.platform.publisher.remoting.client;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.publisher.api.AbstractBasePublishedDocumentFactory;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;

import java.util.Map;

/**
 * Implementation of {@link PublishedDocumentFactory} for local tree pointing to a remote tree. This basically means
 * that the only job done by the {@link PublishedDocumentFactory} is snaphoting the document before sending it for
 * remote publishing.
 *
 * @author tiry
 */
public class ClientProxyFactory extends AbstractBasePublishedDocumentFactory implements PublishedDocumentFactory {

    public PublishedDocument publishDocument(DocumentModel doc, PublicationNode targetNode) {
        throw new IllegalStateException("ClientProxyFactory can not be called to publish locally");
    }

    public PublishedDocument publishDocument(DocumentModel doc, PublicationNode targetNode, Map<String, String> params)
            {
        throw new IllegalStateException("ClientProxyFactory can not be called to publish locally");
    }

    public PublishedDocument wrapDocumentModel(DocumentModel doc) {
        throw new NuxeoException("this factory can not wrap a PublishedDocument");
    }

}
