/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.platform.importer.xml.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Interface for importer service
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public interface XMLImporterService {

    /**
     * Imports {@link DocumentModel} in Nuxeo from an XML or a Zip archive.
     *
     * @param root target container {@link DocumentModel}
     * @param source source file, can be XML or Zip with XML index
     */
    List<DocumentModel> importDocuments(DocumentModel root, File source) throws IOException;


    /**
     * Imports {@link DocumentModel} in Nuxeo from an XML Stream.
     *
     * @param root target container {@link DocumentModel}
     * @param xmlStream stream source for Xml contnt
     */
    List<DocumentModel> importDocuments(DocumentModel root, InputStream xmlStream) throws IOException;

    /**
     * Same as {@link #importDocuments(DocumentModel, File)} with map injected into mvel contexts used during parsing
     *
     * @param root target container {@link DocumentModel}
     * @param source source file, can be XML or Zip with XML index
     * @param mvelContext Context added each time a mvel expression is resolved
     */
    List<DocumentModel> importDocuments(DocumentModel root, File source, Map<String, Object> mvelContext)
            throws IOException;

    /**
     * Same as {@link #importDocuments(DocumentModel, InputStream)} with map injected into mvel contexts used during
     * parsing
     *
     * @param root target container {@link DocumentModel}
     * @param xmlStream stream source for Xml contnt
     * @param mvelContext Context added each time a mvel expression is resolved
     */
    List<DocumentModel> importDocuments(DocumentModel root, InputStream xmlStream,
            Map<String, Object> mvelContext) throws IOException;

    List<DocumentModel> importDocuments(DocumentModel root, File source, Map<String, Object> mvelContext,
			boolean deferSave) throws IOException;

    /**
     * Imports {@link DocumentModel} in Nuxeo from an XML or a Zip archive.
     *
     * @param root target container {@link DocumentModel}
     * @param source source file, can be XML or Zip with XML index
     * @param deferSave if true, do not save docs in docsStack during processing, save them after full parse of xml doc
     * @since 7.4
     */
    List<DocumentModel> importDocuments(DocumentModel root, File source, boolean deferSave) throws IOException;

}
