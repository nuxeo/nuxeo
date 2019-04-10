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
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.TransientRelationship;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;

/**
 * Transient CMIS Relationship for Nuxeo.
 */
public class NuxeoTransientRelationship extends NuxeoTransientObject implements
        TransientRelationship {

    public NuxeoTransientRelationship(NuxeoObject object) {
        super(object);
    }

    @Override
    public ObjectId getSourceId() {
        return ((NuxeoRelationship) object).getSourceId();
    }

    @Override
    public void setSourceId(ObjectId id) {
        if (id == null || id.getId() == null) {
            throw new CmisInvalidArgumentException("Invalid ID");
        }
        setPropertyValue(PropertyIds.SOURCE_ID, id.getId());
    }

    @Override
    public CmisObject getSource() {
        return ((NuxeoRelationship) object).getSource();
    }

    @Override
    public CmisObject getSource(OperationContext context) {
        return ((NuxeoRelationship) object).getSource(context);
    }

    @Override
    public ObjectId getTargetId() {
        return ((NuxeoRelationship) object).getTargetId();
    }

    @Override
    public void setTargetId(ObjectId id) {
        if (id == null || id.getId() == null) {
            throw new CmisInvalidArgumentException("Invalid ID");
        }
        setPropertyValue(PropertyIds.TARGET_ID, id.getId());
    }

    @Override
    public CmisObject getTarget() {
        return ((NuxeoRelationship) object).getTarget();
    }

    @Override
    public CmisObject getTarget(OperationContext context) {
        return ((NuxeoRelationship) object).getTarget(context);
    }

}
