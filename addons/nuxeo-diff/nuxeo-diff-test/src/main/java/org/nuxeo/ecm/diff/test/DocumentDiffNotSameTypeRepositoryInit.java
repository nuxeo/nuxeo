/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer
 */

package org.nuxeo.ecm.diff.test;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Inits the repository for a document diff test case with 2 documents that are not of the same type.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public class DocumentDiffNotSameTypeRepositoryInit extends DocumentDiffRepositoryInit {

    /**
     * Creates the left doc.
     *
     * @param session the session
     * @return the document model
     * @throws ClientException the client exception
     */
    protected DocumentModel createLeftDoc(CoreSession session) throws ClientException {

        DocumentModel doc = session.createDocumentModel("/", "leftDoc", "SampleType");

        // -----------------------
        // dublincore
        // -----------------------
        doc.setPropertyValue("dc:title", "My first sample, of type SampleType.");
        doc.setPropertyValue("dc:description", "description");

        // -----------------------
        // file
        // -----------------------
        doc.setPropertyValue("file:filename", "joe.doc");
        doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob("Joe is bask."));

        // -----------------------
        // simpletypes
        // -----------------------
        doc.setPropertyValue("st:string", "a string property");
        doc.setPropertyValue("st:boolean", true);

        return session.createDocument(doc);
    }

    /**
     * Creates the right doc.
     *
     * @param session the session
     * @return the document model
     * @throws ClientException the client exception
     */
    protected DocumentModel createRightDoc(CoreSession session) throws ClientException {

        DocumentModel doc = session.createDocumentModel("/", "rightDoc", "OtherSampleType");

        // -----------------------
        // dublincore
        // -----------------------
        doc.setPropertyValue("dc:title", "My second sample, of type OtherSampleType.");
        doc.setPropertyValue("dc:description", "Description is different.");

        // -----------------------
        // note
        // -----------------------
        doc.setPropertyValue("note:note", "The note content.");

        // -----------------------
        // simpletypes
        // -----------------------
        doc.setPropertyValue("st:string", "a different string property");
        doc.setPropertyValue("st:boolean", false);

        return session.createDocument(doc);
    }
}
