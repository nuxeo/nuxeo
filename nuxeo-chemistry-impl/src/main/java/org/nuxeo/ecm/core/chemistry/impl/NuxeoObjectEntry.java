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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.chemistry.BaseType;
import org.apache.chemistry.ChangeInfo;
import org.apache.chemistry.ObjectEntry;
import org.apache.chemistry.PropertyDefinition;
import org.apache.chemistry.Type;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

public class NuxeoObjectEntry implements ObjectEntry, DocumentModelHolder {

    private DocumentModel doc;

    private final Type type;

    protected NuxeoObjectEntry(DocumentModel doc, NuxeoConnection connection) {
        this.doc = doc;
        type = connection.repository.getType(NuxeoType.mappedId(doc.getType()));
    }

    // ----- DocumentModelHolder -----
    public void setDocumentModel(DocumentModel doc) {
        this.doc = doc;
    }

    // ----- DocumentModelHolder -----
    public DocumentModel getDocumentModel() {
        return doc;
    }

    public String getId() {
        return doc.getId();
    }

    public String getTypeId() {
        return NuxeoType.mappedId(doc.getType());
    }

    public BaseType getBaseType() {
        return type.getBaseType();
    }

    public ChangeInfo getChangeInfo() {
        return null;
    }

    public String getPathSegment() {
        return doc.getName();
    }

    public Serializable getValue(String id) {
        // TODO avoid constructing property object
        return NuxeoProperty.construct(id, type, this).getValue();
    }

    public void setValue(String id, Serializable value) {
        // TODO avoid constructing property object
        NuxeoProperty.construct(id, type, this).setValue(value);
    }

    public Map<String, Serializable> getValues() {
        Map<String, Serializable> values = new HashMap<String, Serializable>();
        for (PropertyDefinition propertyDefinition : type.getPropertyDefinitions()) {
            String id = propertyDefinition.getId();
            values.put(id, getValue(id));
        }
        return values;
    }

    public void setValues(Map<String, Serializable> values) {
        for (String id : values.keySet()) {
            setValue(id, values.get(id));
        }
    }

    public Set<QName> getAllowableActions() {
        return null;
    }

    public Collection<ObjectEntry> getRelationships() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    protected void save() throws ClientException {
        doc.getCoreSession().saveDocument(doc);
    }

}
