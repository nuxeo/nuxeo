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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Policy;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ContentStreamHash;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConstraintException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisNotSupportedException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamHashImpl;
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
        service.cancelCheckOut(getRepositoryId(), getId(), null);
    }

    @Override
    public ObjectId checkIn(boolean major, Map<String, ?> properties,
            ContentStream contentStream, String checkinComment) {
        return checkIn(major, properties, contentStream, checkinComment, null,
                null, null);
    }

    @Override
    public ObjectId checkIn(boolean major, Map<String, ?> properties,
            ContentStream contentStream, String checkinComment,
            List<Policy> policies, List<Ace> addAces, List<Ace> removeAces) {
        Holder<String> idHolder = new Holder<>(getId());
        service.checkIn(getRepositoryId(), idHolder, Boolean.valueOf(major),
                objectFactory.convertProperties(properties, type, null,
                        UPDATABILITY_READWRITE),
                objectFactory.convertContentStream(contentStream),
                checkinComment, objectFactory.convertPolicies(policies),
                objectFactory.convertAces(addAces),
                objectFactory.convertAces(removeAces), null);
        return session.createObjectId(idHolder.getValue());
    }

    @Override
    public ObjectId checkOut() {
        Holder<String> idHolder = new Holder<>(getId());
        service.checkOut(getRepositoryId(), idHolder, null, null);
        return session.createObjectId(idHolder.getValue());
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
        NuxeoObjectData newData = nuxeoCmisService.copy(getId(), target.getId(),
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
    public NuxeoDocument deleteContentStream() {
        ObjectId objectId = deleteContentStream(true);
        return (NuxeoDocument) session.getObject(objectId);
    }

    @Override
    public ObjectId deleteContentStream(boolean refresh) {
        Holder<String> objectIdHolder = new Holder<String>(getId());
        String changeToken = getPropertyValue(PropertyIds.CHANGE_TOKEN);
        Holder<String> changeTokenHolder = new Holder<String>(changeToken);

        service.deleteContentStream(getRepositoryId(), objectIdHolder,
                changeTokenHolder, null);

        String objectId = objectIdHolder.getValue(); // never null
        return session.createObjectId(objectId);
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
        return getPropertyValue(PropertyIds.CHECKIN_COMMENT);
    }

    @Override
    public ContentStream getContentStream() {
        return getContentStream(null, null, null);
    }

    @Override
    public ContentStream getContentStream(String streamId) {
        return getContentStream(streamId, null, null);
    }

    @Override
    public ContentStream getContentStream(BigInteger offset, BigInteger length) {
        return getContentStream(null, offset, length);
    }

    @Override
    public ContentStream getContentStream(String streamId, BigInteger offset,
            BigInteger length) {
        try {
            return service.getContentStream(getRepositoryId(), getId(),
                    streamId, offset, length, null);
        } catch (CmisConstraintException e) {
            return null;
        }
    }

    @Override
    public String getContentStreamFileName() {
        return getPropertyValue(PropertyIds.CONTENT_STREAM_FILE_NAME);
    }

    @Override
    public String getContentStreamId() {
        return getPropertyValue(PropertyIds.CONTENT_STREAM_ID);
    }

    @Override
    public long getContentStreamLength() {
        Long length = getPropertyValue(PropertyIds.CONTENT_STREAM_LENGTH);
        return length == null ? -1 : length.longValue();
    }

    @Override
    public String getContentStreamMimeType() {
        return getPropertyValue(PropertyIds.CONTENT_STREAM_MIME_TYPE);
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
        return getPropertyValue(PropertyIds.VERSION_LABEL);
    }

    @Override
    public String getVersionSeriesCheckedOutBy() {
        return getPropertyValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_BY);
    }

    @Override
    public String getVersionSeriesCheckedOutId() {
        return getPropertyValue(PropertyIds.VERSION_SERIES_CHECKED_OUT_ID);
    }

    @Override
    public String getVersionSeriesId() {
        return getPropertyValue(PropertyIds.VERSION_SERIES_ID);
    }

    @Override
    public Boolean isImmutable() {
        return getPropertyValue(PropertyIds.IS_IMMUTABLE);
    }

    @Override
    public Boolean isLatestMajorVersion() {
        return getPropertyValue(PropertyIds.IS_LATEST_MAJOR_VERSION);
    }

    @Override
    public Boolean isLatestVersion() {
        return getPropertyValue(PropertyIds.IS_LATEST_VERSION);
    }

    @Override
    public Boolean isMajorVersion() {
        return getPropertyValue(PropertyIds.IS_MAJOR_VERSION);
    }

    @Override
    public Boolean isVersionSeriesCheckedOut() {
        return getPropertyValue(PropertyIds.IS_VERSION_SERIES_CHECKED_OUT);
    }

    @Override
    public Boolean isPrivateWorkingCopy() {
        return getPropertyValue(PropertyIds.IS_PRIVATE_WORKING_COPY);
    }

    @Override
    public Document setContentStream(ContentStream contentStream,
            boolean overwrite) {
        ObjectId objectId = setContentStream(contentStream, overwrite, true);
        return (Document) session.getObject(objectId);
    }

    @Override
    public ObjectId setContentStream(ContentStream contentStream,
            boolean overwrite, boolean refresh) {
        Holder<String> objectIdHolder = new Holder<String>(getId());
        String changeToken = getPropertyValue(PropertyIds.CHANGE_TOKEN);
        Holder<String> changeTokenHolder = new Holder<String>(changeToken);

        service.setContentStream(getRepositoryId(), objectIdHolder,
                Boolean.valueOf(overwrite), changeTokenHolder, contentStream,
                null);

        String objectId = objectIdHolder.getValue(); // never null
        return session.createObjectId(objectId);
    }

    @Override
    public Document appendContentStream(ContentStream contentStream,
            boolean isLastChunk) {
        ObjectId objectId = appendContentStream(contentStream, isLastChunk, true);
        return (Document) session.getObject(objectId);
    }

    @Override
    public ObjectId appendContentStream(ContentStream contentStream,
            boolean isLastChunk, boolean refresh) {
        throw new CmisNotSupportedException();
    }

    @Override
    public List<ContentStreamHash> getContentStreamHashes() {
        List<String> hashes = getPropertyValue(PropertyIds.CONTENT_STREAM_HASH);
        if (hashes == null || hashes.size() == 0) {
            return null;
        }

        List<ContentStreamHash> result = new ArrayList<ContentStreamHash>(hashes.size());
        for (String hash : hashes) {
            result.add(new ContentStreamHashImpl(hash));
        }

        return result;
    }

}
