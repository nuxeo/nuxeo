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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl.client;

import java.util.List;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Relationship;
import org.apache.chemistry.opencmis.client.api.RelationshipType;
import org.apache.chemistry.opencmis.client.api.SecondaryType;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoObjectData;

/**
 * Live local CMIS Relationship, which is backed by a Nuxeo document.
 */
public class NuxeoRelationship extends NuxeoObject implements Relationship {

    public NuxeoRelationship(NuxeoSession session, NuxeoObjectData data, ObjectType type,
            List<SecondaryType> secondaryTypes) {
        super(session, data, type, secondaryTypes);
    }

    @Override
    public RelationshipType getRelationshipType() {
        ObjectType objectType = getType();
        if (objectType instanceof RelationshipType) {
            return (RelationshipType) objectType;
        } else {
            throw new ClassCastException("Object type is not a relationship type.");
        }
    }

    @Override
    public ObjectId getSourceId() {
        String id = getPropertyValue(PropertyIds.SOURCE_ID);
        return id == null ? null : session.createObjectId(id);
    }

    @Override
    public CmisObject getSource() {
        String id = getPropertyValue(PropertyIds.SOURCE_ID);
        return id == null ? null : session.getObject(session.createObjectId(id));
    }

    @Override
    public CmisObject getSource(OperationContext context) {
        String id = getPropertyValue(PropertyIds.SOURCE_ID);
        return id == null ? null : session.getObject(session.createObjectId(id), context);
    }

    @Override
    public ObjectId getTargetId() {
        String id = getPropertyValue(PropertyIds.TARGET_ID);
        return id == null ? null : session.createObjectId(id);
    }

    @Override
    public CmisObject getTarget() {
        String id = getPropertyValue(PropertyIds.TARGET_ID);
        return id == null ? null : session.getObject(session.createObjectId(id));
    }

    @Override
    public CmisObject getTarget(OperationContext context) {
        String id = getPropertyValue(PropertyIds.TARGET_ID);
        return id == null ? null : session.getObject(session.createObjectId(id), context);
    }

}
