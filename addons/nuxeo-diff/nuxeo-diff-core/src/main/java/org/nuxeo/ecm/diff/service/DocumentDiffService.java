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
 *     ataillefer
 */
package org.nuxeo.ecm.diff.service;

import java.io.Serializable;

import org.custommonkey.xmlunit.Diff;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.diff.model.DocumentDiff;

/**
 * Handles a diff between two documents.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public interface DocumentDiffService extends Serializable {

    /**
     * Makes a diff between leftDoc and rightDoc. Returns a DocumentDiff object that wraps the differences, schema by
     * schema and field by field.
     *
     * @param session the session
     * @param leftDoc the left doc
     * @param rightDoc the right doc
     * @return the document diff
     */
    DocumentDiff diff(CoreSession session, DocumentModel leftDoc, DocumentModel rightDoc);

    /**
     * Makes a diff between leftXML and rightXML. Returns a DocumentDiff object that wraps the differences, schema by
     * schema and field by field.
     *
     * @param leftXML the left XML
     * @param rightXML the right XML
     * @return the document diff
     */
    DocumentDiff diff(String leftXML, String rightXML);

    /**
     * Configures XMLUnit.
     */
    void configureXMLUnit();

    /**
     * Configures the diff.
     *
     * @param diff the diff
     */
    void configureDiff(Diff diff);

}
