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
import java.util.Collection;
import java.util.List;

import org.apache.chemistry.CMISObject;
import org.apache.chemistry.CMISRuntimeException;
import org.apache.chemistry.Document;
import org.apache.chemistry.Folder;
import org.apache.chemistry.ObjectId;
import org.apache.chemistry.Unfiling;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

public class NuxeoFolder extends NuxeoObject implements Folder {

    public final String name;

    public NuxeoFolder(DocumentModel doc, NuxeoConnection connection) {
        super(doc, connection);
        name = null;
    }

    // for the root folder we need to force the name
    public NuxeoFolder(DocumentModel doc, NuxeoConnection connection,
            String name) {
        super(doc, connection);
        this.name = name;
    }

    // TODO override properties etc.
    @Override
    public String getName() {
        return name == null ? super.getName() : name;
    }

    public void add(CMISObject object) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void remove(CMISObject object) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Collection<ObjectId> deleteTree(Unfiling unfiling) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public List<CMISObject> getChildren() {
        DocumentModelList docs;
        try {
            docs = connection.session.getChildren(doc.getRef(), null,
                    connection.getDocumentFilter(), null);
        } catch (ClientException e) {
            throw new CMISRuntimeException(e);
        }
        if (docs == null) {
            throw new IllegalArgumentException(doc.getId());
        }
        List<CMISObject> children = new ArrayList<CMISObject>(docs.size());
        for (DocumentModel child : docs) {
            children.add(NuxeoObject.construct(child, connection));
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
