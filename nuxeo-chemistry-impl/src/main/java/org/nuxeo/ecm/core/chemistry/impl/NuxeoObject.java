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

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.apache.chemistry.BaseType;
import org.apache.chemistry.CMISObject;
import org.apache.chemistry.ContentStream;
import org.apache.chemistry.Folder;
import org.apache.chemistry.Policy;
import org.apache.chemistry.Property;
import org.apache.chemistry.Relationship;
import org.apache.chemistry.RelationshipDirection;
import org.apache.chemistry.Type;
import org.apache.chemistry.impl.base.BaseObject;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

public class NuxeoObject extends BaseObject implements CMISObject {

    protected DocumentModel doc;

    protected NuxeoConnection connection;

    protected NuxeoObject(DocumentModel doc, NuxeoConnection connection) {
        this.doc = doc;
        this.connection = connection;
    }

    protected static NuxeoObject construct(DocumentModel doc,
            NuxeoConnection connection) {
        if (doc.isFolder()) {
            return new NuxeoFolder(doc, connection);
        } else {
            return new NuxeoDocument(doc, connection);
        }
    }

    public void move(Folder targetFolder, Folder sourceFolder) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void delete() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void unfile() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Folder getParent() {
        if (doc.getPathAsString().equals("/")) {
            return null;
        }
        DocumentRef parentRef = doc.getParentRef();
        if (parentRef == null) {
            return null;
        }
        try {
            return new NuxeoFolder(connection.session.getDocument(parentRef),
                    connection);
        } catch (ClientException e) {
            throw new RuntimeException(e.toString(), e); // TODO
        }
    }

    public Collection<Folder> getParents() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public List<Relationship> getRelationships(RelationshipDirection direction,
            String typeId, boolean includeSubRelationshipTypes) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void applyPolicy(Policy policy) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void removePolicy(Policy policy) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Collection<Policy> getPolicies() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Type getType() {
        return connection.repository.getType(NuxeoType.mappedId(doc.getType()));
    }

    public BaseType getBaseType() {
        return getType().getBaseType();
    }

    public Serializable getValue(String name) {
        // TODO avoid constructing property object
        return getProperty(name).getValue();
    }

    public Property getProperty(String name) {
        try {
            return NuxeoProperty.getProperty(doc, getType(), name,
                    connection.session);
        } catch (ClientException e) {
            throw new RuntimeException(e.toString(), e); // TODO
        }
    }

    public ContentStream getContentStream(String contentStreamId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void save() {
        try {
            if (getId() == null) {
                if (getName() == null) {
                    throw new IllegalArgumentException("Missing name");
                }
                connection.session.createDocument(doc);
            } else {
                connection.session.saveDocument(doc);
            }
            connection.session.save();
        } catch (ClientException e) {
            throw new RuntimeException("Cannot save: " + e, e); // TODO
        }
    }

}
