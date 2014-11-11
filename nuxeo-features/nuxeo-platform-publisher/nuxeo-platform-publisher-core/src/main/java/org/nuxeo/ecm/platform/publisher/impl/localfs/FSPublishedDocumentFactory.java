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

package org.nuxeo.ecm.platform.publisher.impl.localfs;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.publisher.api.AbstractBasePublishedDocumentFactory;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;

import java.util.Map;

public class FSPublishedDocumentFactory extends
        AbstractBasePublishedDocumentFactory implements
        PublishedDocumentFactory {

    public PublishedDocument publishDocument(DocumentModel doc,
            PublicationNode targetNode, Map<String, String> params)
            throws ClientException {

        try {
            FSPublishedDocument pubDoc = new FSPublishedDocument("local", doc);
            pubDoc.persist(targetNode.getPath());
            return pubDoc;
        } catch (Exception e) {
            throw new ClientException("Error duning FS Publishing", e);
        }
    }

    public PublishedDocument wrapDocumentModel(DocumentModel doc)
            throws ClientException {
        try {

            doc = snapshotDocumentBeforePublish(doc);
            return new FSPublishedDocument("local", doc);
        } catch (Exception e) {
            throw new ClientException(
                    "Error while wrapping DocumentModel as FSPublishedDocument",
                    e);
        }
    }

}
