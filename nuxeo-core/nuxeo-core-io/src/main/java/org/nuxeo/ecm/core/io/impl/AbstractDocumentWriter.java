/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: AbstractDocumentWriter.java 29029 2008-01-14 18:38:14Z ldoguin $
 */

package org.nuxeo.ecm.core.io.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.dom4j.io.OutputFormat;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.io.DocumentTranslationMap;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.ExportedDocument;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class AbstractDocumentWriter implements DocumentWriter {

    // this abstract method is needed
    @Override
    public abstract DocumentTranslationMap write(ExportedDocument doc) throws IOException;

    @Override
    public DocumentTranslationMap write(ExportedDocument[] docs) throws IOException {
        if (docs == null || docs.length == 0) {
            return null;
        }
        String newRepo = null;
        String oldRepo = null;
        Map<DocumentRef, DocumentRef> newRefs = new HashMap<>();
        for (ExportedDocument doc : docs) {
            DocumentTranslationMap newMap = write(doc);
            if (newMap != null) {
                newRefs.putAll(newMap.getDocRefMap());
                // assume repo will be the same for all docs
                if (oldRepo == null) {
                    oldRepo = newMap.getOldServerName();
                }
                if (newRepo == null) {
                    newRepo = newMap.getNewServerName();
                }
            }
        }
        return new DocumentTranslationMapImpl(oldRepo, newRepo, newRefs);
    }

    @Override
    public DocumentTranslationMap write(Collection<ExportedDocument> docs) throws IOException {
        if (docs == null || docs.isEmpty()) {
            return null;
        }
        String newRepo = null;
        String oldRepo = null;
        Map<DocumentRef, DocumentRef> newRefs = new HashMap<>();
        for (ExportedDocument doc : docs) {
            DocumentTranslationMap newMap = write(doc);
            if (newMap != null) {
                newRefs.putAll(newMap.getDocRefMap());
                // assume repo will be the same for all docs
                if (oldRepo == null) {
                    oldRepo = newMap.getOldServerName();
                }
                if (newRepo == null) {
                    newRepo = newMap.getNewServerName();
                }
            }
        }
        return new DocumentTranslationMapImpl(oldRepo, newRepo, newRefs);
    }

    public static OutputFormat createPrettyPrint() {
        OutputFormat format = new OutputFormat();
        format.setIndentSize(2);
        format.setNewlines(true);
        return format;
    }

    public static OutputFormat createCompactFormat() {
        OutputFormat format = new OutputFormat();
        format.setIndent(false);
        format.setNewlines(false);
        return format;
    }

}
