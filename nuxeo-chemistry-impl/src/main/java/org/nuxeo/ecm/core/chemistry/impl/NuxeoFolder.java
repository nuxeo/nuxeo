/*
 * Copyright 2009 Nuxeo SA <http://nuxeo.com>
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
 * Authors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.chemistry.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.chemistry.Document;
import org.apache.chemistry.Folder;
import org.apache.chemistry.ObjectEntry;
import org.apache.chemistry.type.BaseType;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

public class NuxeoFolder extends NuxeoObject implements Folder {

    public NuxeoFolder(DocumentModel doc, NuxeoConnection connection) {
        super(doc, connection);
    }

    public List<ObjectEntry> getChildren(BaseType type, String orderBy) {
        // TODO type and orderBy
        DocumentModelList docs;
        try {
            docs = connection.session.getChildren(doc.getRef());
        } catch (ClientException e) {
            throw new RuntimeException(e.toString(), e); // TODO
        }
        if (docs == null) {
            throw new IllegalArgumentException(doc.getId());
        }
        List<ObjectEntry> children = new ArrayList<ObjectEntry>(docs.size());
        for (DocumentModel child : docs) {
            children.add(new NuxeoObjectEntry(child, connection));
        }
        return children;
    }

    public Document newDocument(String typeId) {
        return connection.newDocument(typeId, this);
    }

    public Folder newFolder(String typeId) {
        return connection.newFolder(typeId, this);
    }

}
