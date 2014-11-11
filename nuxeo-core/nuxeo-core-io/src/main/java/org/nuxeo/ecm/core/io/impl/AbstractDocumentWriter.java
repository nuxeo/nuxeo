/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: AbstractDocumentWriter.java 29029 2008-01-14 18:38:14Z ldoguin $
 */

package org.nuxeo.ecm.core.io.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.io.DocumentTranslationMap;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.ExportedDocument;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class AbstractDocumentWriter implements DocumentWriter {

    // this abstract method is needed
    @Override
    public abstract DocumentTranslationMap write(ExportedDocument doc)
            throws IOException;

    @Override
    public DocumentTranslationMap write(ExportedDocument[] docs)
            throws IOException {
        if (docs == null || docs.length == 0) {
            return null;
        }
        String newRepo = null;
        String oldRepo = null;
        Map<DocumentRef, DocumentRef> newRefs = new HashMap<DocumentRef, DocumentRef>();
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
    public DocumentTranslationMap write(Collection<ExportedDocument> docs)
            throws IOException {
        if (docs == null || docs.isEmpty()) {
            return null;
        }
        String newRepo = null;
        String oldRepo = null;
        Map<DocumentRef, DocumentRef> newRefs = new HashMap<DocumentRef, DocumentRef>();
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

}
