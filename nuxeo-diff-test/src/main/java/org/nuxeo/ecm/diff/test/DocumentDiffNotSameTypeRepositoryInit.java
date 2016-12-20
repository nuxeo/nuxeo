/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */

package org.nuxeo.ecm.diff.test;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.Blobs;
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
     */
    protected DocumentModel createLeftDoc(CoreSession session) {

        DocumentModel doc = session.createDocumentModel("/", "leftDoc", "SampleType");

        // -----------------------
        // dublincore
        // -----------------------
        doc.setPropertyValue("dc:title", "My first sample, of type SampleType.");
        doc.setPropertyValue("dc:description", "description");

        // -----------------------
        // file
        // -----------------------
        doc.setPropertyValue("file:content",
                (Serializable) Blobs.createBlob("Joe is bask.", "text/plain", "UTF-8", "joe.doc"));

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
     */
    protected DocumentModel createRightDoc(CoreSession session) {

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
