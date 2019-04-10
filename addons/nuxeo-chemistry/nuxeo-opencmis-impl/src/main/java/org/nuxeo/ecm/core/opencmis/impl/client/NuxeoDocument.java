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

import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Policy;
import org.apache.chemistry.opencmis.client.api.Relationship;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.RelationshipDirection;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConstraintException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.spi.Holder;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoObjectData;

/**
 * Live local CMIS Document, which is backed by a Nuxeo document.
 */
public class NuxeoDocument extends NuxeoFileableObject implements Document {

    public NuxeoDocument(NuxeoSession session, NuxeoObjectData data,
            ObjectType type) {
        super(session, data, type);
    }

    @Override
    public void cancelCheckOut() {
        service.cancelCheckOut(getId());
    }

    @Override
    public ObjectId checkIn(boolean major, Map<String, ?> properties,
            ContentStream contentStream, String checkinComment) {
        String verId = service.checkIn(getId(), major, properties, type,
                contentStream, checkinComment);
        return session.createObjectId(verId);
    }

    @Override
    public ObjectId checkIn(boolean major, Map<String, ?> properties,
            ContentStream contentStream, String checkinComment,
            List<Policy> policies, List<Ace> addAces, List<Ace> removeAces) {
        // TODO policies, addAces, removeAces
        return checkIn(major, properties, contentStream, checkinComment);
    }

    @Override
    public ObjectId checkOut() {
        String pwcId = service.checkOut(getId());
        return session.createObjectId(pwcId);
    }

    @Override
    public NuxeoDocument copy(ObjectId target) {
        return copy(target, null, null, null, null, null,
                session.getDefaultContext());
    }

    @Override
    public NuxeoDocument copy(ObjectId target, Map<String, ?> properties,
            VersioningState versioningState, List<Policy> policies,
            List<Ace> addACEs, List<Ace> removeACEs, OperationContext context) {
        if (target == null || target.getId() == null) {
            throw new CmisInvalidArgumentException("Invalid target: " + target);
        }
        if (context == null) {
            context = session.getDefaultContext();
        }
        NuxeoObjectData newData = service.copy(getId(), target.getId(),
                properties, type, versioningState, policies, addACEs,
                removeACEs, context);
        return (NuxeoDocument) session.getObjectFactory().convertObject(
                newData, context);
    }

    @Override
    public void deleteAllVersions() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectId deleteContentStream() {
        Holder<String> objectIdHolder = new Holder<String>(getId());
        String changeToken = getPropertyValue(PropertyIds.CHANGE_TOKEN);
        Holder<String> changeTokenHolder = new Holder<String>(changeToken);

        service.deleteContentStream(getRepositoryId(), objectIdHolder,
                changeTokenHolder, null);

        String objectId = objectIdHolder.getValue();
        return objectId == null ? null : session.createObjectId(objectId);
    }

    @Override
    public List<Document> getAllVersions() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Document> getAllVersions(OperationContext context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCheckinComment() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ContentStream getContentStream() {
        return getContentStream(null);
    }

    @Override
    public ContentStream getContentStream(String streamId) {
        try {
            return service.getContentStream(getRepositoryId(), getId(),
                    streamId, null, null, null);
        } catch (CmisConstraintException e) {
            return null;
        }
    }

    @Override
    public String getContentStreamFileName() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public String getContentStreamId() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public long getContentStreamLength() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public String getContentStreamMimeType() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Document getObjectOfLatestVersion(boolean major) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Document getObjectOfLatestVersion(boolean major,
            OperationContext context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public String getVersionLabel() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public String getVersionSeriesCheckedOutBy() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public String getVersionSeriesCheckedOutId() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public String getVersionSeriesId() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean isImmutable() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean isLatestMajorVersion() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean isLatestVersion() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean isMajorVersion() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean isVersionSeriesCheckedOut() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectId setContentStream(ContentStream contentStream,
            boolean overwrite) {
        Holder<String> objectIdHolder = new Holder<String>(getId());
        String changeToken = getPropertyValue(PropertyIds.CHANGE_TOKEN);
        Holder<String> changeTokenHolder = new Holder<String>(changeToken);

        service.setContentStream(getRepositoryId(), objectIdHolder,
                Boolean.valueOf(overwrite), changeTokenHolder, contentStream,
                null);

        String objectId = objectIdHolder.getValue();
        return objectId == null ? null : session.createObjectId(objectId);
    }

    @Override
    public ItemIterable<Relationship> getRelationships(
            boolean includeSubRelationshipTypes,
            RelationshipDirection relationshipDirection, ObjectType type,
            OperationContext context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

}