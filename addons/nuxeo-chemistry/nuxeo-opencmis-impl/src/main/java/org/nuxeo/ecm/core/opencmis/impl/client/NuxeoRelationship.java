/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl.client;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Relationship;
import org.apache.chemistry.opencmis.client.api.TransientRelationship;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoObjectData;

/**
 * Live local CMIS Relationship, which is backed by a Nuxeo document.
 */
public class NuxeoRelationship extends NuxeoObject implements Relationship {

    public NuxeoRelationship(NuxeoSession session, NuxeoObjectData data,
            ObjectType type) {
        super(session, data, type);
    }

    @Override
    public TransientRelationship getTransientRelationship() {
        return (TransientRelationship) getAdapter(TransientRelationship.class);
    }

    @Override
    public ObjectId getSourceId() {
        String id = getPropertyValue(PropertyIds.SOURCE_ID);
        return id == null ? null : session.createObjectId(id);
    }

    @Override
    public CmisObject getSource() {
        String id = getPropertyValue(PropertyIds.SOURCE_ID);
        return id == null ? null
                : session.getObject(session.createObjectId(id));
    }

    @Override
    public CmisObject getSource(OperationContext context) {
        String id = getPropertyValue(PropertyIds.SOURCE_ID);
        return id == null ? null : session.getObject(
                session.createObjectId(id), context);
    }

    @Override
    public ObjectId getTargetId() {
        String id = getPropertyValue(PropertyIds.TARGET_ID);
        return id == null ? null : session.createObjectId(id);
    }

    @Override
    public CmisObject getTarget() {
        String id = getPropertyValue(PropertyIds.TARGET_ID);
        return id == null ? null
                : session.getObject(session.createObjectId(id));
    }

    @Override
    public CmisObject getTarget(OperationContext context) {
        String id = getPropertyValue(PropertyIds.TARGET_ID);
        return id == null ? null : session.getObject(
                session.createObjectId(id), context);
    }

}
