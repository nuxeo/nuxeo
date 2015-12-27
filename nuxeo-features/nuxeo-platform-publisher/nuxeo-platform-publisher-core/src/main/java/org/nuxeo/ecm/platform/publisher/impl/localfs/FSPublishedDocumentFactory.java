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

package org.nuxeo.ecm.platform.publisher.impl.localfs;

import java.io.IOException;
import java.util.Map;

import org.dom4j.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.publisher.api.AbstractBasePublishedDocumentFactory;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;

public class FSPublishedDocumentFactory extends AbstractBasePublishedDocumentFactory implements
        PublishedDocumentFactory {

    public PublishedDocument publishDocument(DocumentModel doc, PublicationNode targetNode, Map<String, String> params)
            {

        try {
            FSPublishedDocument pubDoc = new FSPublishedDocument("local", doc);
            pubDoc.persist(targetNode.getPath());
            return pubDoc;
        } catch (DocumentException | IOException e) {
            throw new NuxeoException("Error duning FS Publishing", e);
        }
    }

    public PublishedDocument wrapDocumentModel(DocumentModel doc) {
        try {

            doc = snapshotDocumentBeforePublish(doc);
            return new FSPublishedDocument("local", doc);
        } catch (DocumentException e) {
            throw new NuxeoException("Error while wrapping DocumentModel as FSPublishedDocument", e);
        }
    }

}
