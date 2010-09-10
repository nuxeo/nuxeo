/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl.client;

import java.util.ArrayList;
import java.util.List;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ObjectParentData;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.data.PropertyId;
import org.apache.chemistry.opencmis.commons.enums.ExtensionLevel;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoObjectData;

/**
 * Base abstract fileable live local CMIS Object.
 */
public abstract class NuxeoFileableObject extends NuxeoObject implements
        FileableCmisObject {

    public NuxeoFileableObject(NuxeoSession session, NuxeoObjectData data,
            ObjectType type) {
        super(session, data, type);
    }

    @Override
    public void addToFolder(ObjectId folderId, boolean allVersions) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Folder> getParents() {
        String objectId = getId();
        List<ObjectParentData> parentsData = service.getObjectParents(
                getRepositoryId(), objectId, PropertyIds.OBJECT_ID,
                Boolean.FALSE, IncludeRelationships.NONE, null, Boolean.FALSE,
                null);
        List<Folder> parents = new ArrayList<Folder>(parentsData.size());
        for (ObjectParentData p : parentsData) {
            if (p == null || p.getObject() == null
                    || p.getObject().getProperties() == null) {
                throw new CmisRuntimeException("Invalid object");
            }
            PropertyData<?> idProp = p.getObject().getProperties().getProperties().get(
                    PropertyIds.OBJECT_ID);
            if (!(idProp instanceof PropertyId)) {
                throw new CmisRuntimeException("Invalid type");
            }
            String id = (String) idProp.getFirstValue();
            CmisObject parent = session.getObject(session.createObjectId(id));
            if (!(parent instanceof Folder)) {
                throw new CmisRuntimeException("Should be a Folder: "
                        + parent.getClass().getName());
            }
            parents.add((Folder) parent);
        }
        return parents;
    }

    @Override
    public List<String> getPaths() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public FileableCmisObject move(ObjectId sourceFolderId,
            ObjectId targetFolderId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeFromFolder(ObjectId folderId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Object> getExtensions(ExtensionLevel level) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

}