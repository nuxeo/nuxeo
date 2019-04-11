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

import java.util.Collections;
import java.util.List;

import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.SecondaryType;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.spi.Holder;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoObjectData;

/**
 * Base abstract fileable live local CMIS Object.
 */
public abstract class NuxeoFileableObject extends NuxeoObject implements FileableCmisObject {

    public NuxeoFileableObject(NuxeoSession session, NuxeoObjectData data, ObjectType type,
            List<SecondaryType> secondaryTypes) {
        super(session, data, type, secondaryTypes);
    }

    @Override
    public List<Folder> getParents(OperationContext context) {
        // context ignored
        CoreSession coreSession = data.doc.getCoreSession();
        DocumentModel parent = coreSession.getParentDocument(new IdRef(getId()));
        if (parent == null || nuxeoCmisService.isFilteredOut(parent)) {
            return Collections.emptyList();
        }
        Folder folder = (Folder) session.getObject(parent, session.getDefaultContext());
        return Collections.singletonList(folder);
    }

    @Override
    public List<Folder> getParents() {
        return getParents(null);
    }

    @Override
    public List<String> getPaths() {
        return Collections.singletonList(data.doc.getPathAsString());
    }

    @Override
    public void addToFolder(ObjectId folderId, boolean allVersions) {
        throw new UnsupportedOperationException("Multi-filing not supported");
    }

    @Override
    public void removeFromFolder(ObjectId folderId) {
        service.removeObjectFromFolder(getRepositoryId(), getId(), folderId == null ? null : folderId.getId(), null);
    }

    @Override
    public NuxeoFileableObject move(ObjectId sourceFolder, ObjectId targetFolder, OperationContext context) {
        // context ignored
        Holder<String> objectIdHolder = new Holder<>(getId());
        if (sourceFolder == null) {
            throw new CmisInvalidArgumentException("Missing source folder");
        }
        if (targetFolder == null) {
            throw new CmisInvalidArgumentException("Missing target folder");
        }
        service.moveObject(getRepositoryId(), objectIdHolder, targetFolder.getId(), sourceFolder.getId(), null);
        return this;
    }

    @Override
    public NuxeoFileableObject move(ObjectId sourceFolder, ObjectId targetFolder) {
        return move(sourceFolder, targetFolder, null);
    }

}
