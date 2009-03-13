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
import java.util.Map;

import org.apache.chemistry.CMISObject;
import org.apache.chemistry.Document;
import org.apache.chemistry.Folder;
import org.apache.chemistry.Policy;
import org.apache.chemistry.Relationship;
import org.apache.chemistry.property.Property;
import org.apache.chemistry.type.BaseType;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

public class NuxeoObject extends NuxeoObjectEntry implements CMISObject {

    protected NuxeoObject(DocumentModel doc, NuxeoConnection connection) {
        super(doc, connection);
    }

    @Override
    public Document getDocument() {
        if (getType().getBaseType() != BaseType.DOCUMENT) {
            throw new RuntimeException("Not a document: " + getId());
        }
        return (Document) this;
    }

    @Override
    public Folder getFolder() {
        if (getType().getBaseType() != BaseType.FOLDER) {
            throw new RuntimeException("Not a folder: " + getId());
        }
        return (Folder) this;
    }

    @Override
    public Relationship getRelationship() {
        if (getType().getBaseType() != BaseType.RELATIONSHIP) {
            throw new RuntimeException("Not a relationship: " + getId());
        }
        return (Relationship) this;
    }

    @Override
    public Policy getPolicy() {
        if (getType().getBaseType() != BaseType.POLICY) {
            throw new RuntimeException("Not a policy: " + getId());
        }
        return (Policy) this;
    }

    /*
     * ----- CMISObject -----
     */

    public Folder getParent() {
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

    public void setValue(String name, Serializable value) {
        NuxeoPropertyDefinition pd = (NuxeoPropertyDefinition) getType().getPropertyDefinition(
                name);
        if (pd == null) {
            throw new IllegalArgumentException(name);
        }
        String error = pd.validationError(value);
        if (error != null) {
            throw new RuntimeException("Property " + name + ": " + error); // TODO
        }
        getProperty(name).setValue(value);
    }

    public void setValues(Map<String, Serializable> values) {
        // don't use putAll as we want to do type checks
        for (String name : values.keySet()) {
            setValue(name, values.get(name));
        }
    }

    public void save() {
        try {
            if (getId() == null) {
                connection.session.createDocument(doc);
            } else {
                connection.session.saveDocument(doc);
            }
            connection.session.save();
        } catch (ClientException e) {
            throw new RuntimeException("Cannot save: " + e, e);
        }
    }

    /*
     * ----- convenience methods for specific properties -----
     */

    public void setName(String name) {
        setValue(Property.NAME, name);
    }

}
