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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.chemistry.AllowableAction;
import org.apache.chemistry.BaseType;
import org.apache.chemistry.ChangeInfo;
import org.apache.chemistry.ObjectEntry;
import org.apache.chemistry.PropertyDefinition;
import org.apache.chemistry.Type;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.SecurityConstants;

public class NuxeoObjectEntry implements ObjectEntry, DocumentModelHolder {

    private DocumentModel doc;

    private final Type type;

    private final boolean canWrite;

    protected NuxeoObjectEntry(DocumentModel doc, NuxeoConnection connection) {
        this(doc, connection, false);
    }

    protected NuxeoObjectEntry(DocumentModel doc, NuxeoConnection connection,
            boolean creation) {
        this.doc = doc;
        type = connection.repository.getType(NuxeoType.mappedId(doc.getType()));
        // connection is not stored as the ObjectEntry must be stateless
        boolean canWrite;
        try {
            canWrite = creation
                    || connection.session.hasPermission(doc.getRef(),
                            SecurityConstants.WRITE);
        } catch (ClientException e) {
            canWrite = false;
        }
        // TODO more fine-grained permissions
        this.canWrite = canWrite;
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
        boolean isFolder = doc.isFolder();
        Set<QName> set = new HashSet<QName>();
        set.add(AllowableAction.CAN_GET_OBJECT_PARENTS);
        set.add(AllowableAction.CAN_GET_PROPERTIES);
        if (isFolder) {
            set.add(AllowableAction.CAN_GET_DESCENDANTS);
            set.add(AllowableAction.CAN_GET_FOLDER_PARENT);
            set.add(AllowableAction.CAN_GET_FOLDER_TREE);
            set.add(AllowableAction.CAN_GET_CHILDREN);
        } else {
            set.add(AllowableAction.CAN_GET_CONTENT_STREAM);
        }
        if (canWrite) {
            if (isFolder) {
                set.add(AllowableAction.CAN_CREATE_DOCUMENT);
                set.add(AllowableAction.CAN_CREATE_FOLDER);
                set.add(AllowableAction.CAN_CREATE_RELATIONSHIP);
                set.add(AllowableAction.CAN_DELETE_TREE);
                set.add(AllowableAction.CAN_ADD_OBJECT_TO_FOLDER);
                set.add(AllowableAction.CAN_REMOVE_OBJECT_FROM_FOLDER);
            } else {
                set.add(AllowableAction.CAN_SET_CONTENT_STREAM);
                set.add(AllowableAction.CAN_DELETE_CONTENT_STREAM);
            }
            set.add(AllowableAction.CAN_UPDATE_PROPERTIES);
            set.add(AllowableAction.CAN_MOVE_OBJECT);
            set.add(AllowableAction.CAN_DELETE_OBJECT);
        }
        if (Boolean.FALSE.booleanValue()) {
            // TODO
            set.add(AllowableAction.CAN_GET_RENDITIONS);
            set.add(AllowableAction.CAN_CHECK_OUT);
            set.add(AllowableAction.CAN_CANCEL_CHECK_OUT);
            set.add(AllowableAction.CAN_CHECK_IN);
            set.add(AllowableAction.CAN_GET_ALL_VERSIONS);
            set.add(AllowableAction.CAN_GET_OBJECT_RELATIONSHIPS);
            set.add(AllowableAction.CAN_APPLY_POLICY);
            set.add(AllowableAction.CAN_REMOVE_POLICY);
            set.add(AllowableAction.CAN_GET_APPLIED_POLICIES);
            set.add(AllowableAction.CAN_GET_ACL);
            set.add(AllowableAction.CAN_APPLY_ACL);
        }
        return set;
    }

    public Collection<ObjectEntry> getRelationships() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    protected void save() throws ClientException {
        doc.getCoreSession().saveDocument(doc);
    }

}
