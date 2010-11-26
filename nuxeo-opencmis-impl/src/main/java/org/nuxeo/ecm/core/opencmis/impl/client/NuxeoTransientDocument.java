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
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Policy;
import org.apache.chemistry.opencmis.client.api.TransientDocument;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;

/**
 * Transient CMIS Document for Nuxeo.
 */
public class NuxeoTransientDocument extends NuxeoTransientFileableObject
        implements TransientDocument {

    protected ContentStream contentStream;

    protected boolean contentStreamOverwrite;

    protected boolean contentStreamUpdated;

    public NuxeoTransientDocument(NuxeoObject object) {
        super(object);
    }

    @Override
    public boolean isModified() {
        return contentStreamUpdated || super.isModified();
    }

    @Override
    public void deleteAllVersions() {
        delete(true);
    }

    @Override
    public ContentStream getContentStream() {
        if (contentStreamUpdated) {
            return contentStream;
        }
        return ((NuxeoDocument) object).getContentStream();
    }

    @Override
    public ContentStream getContentStream(String streamId) {
        if (streamId == null) {
            return getContentStream();
        }
        return ((NuxeoDocument) object).getContentStream(streamId);
    }

    @Override
    public void setContentStream(ContentStream contentStream, boolean overwrite) {
        this.contentStream = contentStream;
        this.contentStreamOverwrite = overwrite;
        contentStreamUpdated = true;
    }

    @Override
    public void deleteContentStream() {
        this.contentStream = null;
        contentStreamUpdated = true;
    }

    @Override
    public Document getObjectOfLatestVersion(boolean major) {
        return ((NuxeoDocument) object).getObjectOfLatestVersion(major);
    }

    @Override
    public Document getObjectOfLatestVersion(boolean major,
            OperationContext context) {
        return ((NuxeoDocument) object).getObjectOfLatestVersion(major, context);
    }

    @Override
    public List<Document> getAllVersions() {
        return ((NuxeoDocument) object).getAllVersions();
    }

    @Override
    public List<Document> getAllVersions(OperationContext context) {
        return ((NuxeoDocument) object).getAllVersions(context);
    }

    @Override
    public Document copy(ObjectId targetFolderId) {
        return ((NuxeoDocument) object).copy(targetFolderId);
    }

    @Override
    public Document copy(ObjectId targetFolderId, Map<String, ?> properties,
            VersioningState versioningState, List<Policy> policies,
            List<Ace> addACEs, List<Ace> removeACEs, OperationContext context) {
        return ((NuxeoDocument) object).copy(targetFolderId, properties,
                versioningState, policies, addACEs, removeACEs, context);
    }

    @Override
    public Boolean isImmutable() {
        return ((NuxeoDocument) object).isImmutable();
    }

    @Override
    public Boolean isLatestVersion() {
        return ((NuxeoDocument) object).isLatestVersion();
    }

    @Override
    public Boolean isMajorVersion() {
        return ((NuxeoDocument) object).isMajorVersion();
    }

    @Override
    public Boolean isLatestMajorVersion() {
        return ((NuxeoDocument) object).isLatestMajorVersion();
    }

    @Override
    public String getVersionLabel() {
        return ((NuxeoDocument) object).getVersionLabel();
    }

    @Override
    public String getVersionSeriesId() {
        return ((NuxeoDocument) object).getVersionSeriesId();
    }

    @Override
    public Boolean isVersionSeriesCheckedOut() {
        return ((NuxeoDocument) object).isVersionSeriesCheckedOut();
    }

    @Override
    public String getVersionSeriesCheckedOutBy() {
        return ((NuxeoDocument) object).getVersionSeriesCheckedOutBy();
    }

    @Override
    public String getVersionSeriesCheckedOutId() {
        return ((NuxeoDocument) object).getVersionSeriesCheckedOutId();
    }

    @Override
    public String getCheckinComment() {
        return ((NuxeoDocument) object).getCheckinComment();
    }

    @Override
    public long getContentStreamLength() {
        if (contentStreamUpdated) {
            return contentStream == null ? -1 : contentStream.getLength();
        }
        return ((NuxeoDocument) object).getContentStreamLength();
    }

    @Override
    public String getContentStreamMimeType() {
        if (contentStreamUpdated) {
            return contentStream == null ? null : contentStream.getMimeType();
        }
        return ((NuxeoDocument) object).getContentStreamMimeType();
    }

    @Override
    public String getContentStreamFileName() {
        if (contentStreamUpdated) {
            return contentStream == null ? null : contentStream.getFileName();
        }
        return ((NuxeoDocument) object).getContentStreamFileName();
    }

    @Override
    public String getContentStreamId() {
        return ((NuxeoDocument) object).getContentStreamId();
    }

    @Override
    public ObjectId checkIn(boolean major, String checkinComment) {
        ObjectId objectId = ((NuxeoDocument) object).checkIn(major, properties,
                contentStream, checkinComment);
        reset();
        return objectId;
    }

    @Override
    public ObjectId save() {
        if (saveDeletes()) {
            reset();
            return null;
        }
        if (contentStreamUpdated) {
            ((NuxeoDocument) object).setContentStream(contentStream,
                    contentStreamOverwrite);
        }
        return super.save(); // update properties, reset
    }

    @Override
    public void reset() {
        super.reset();
        contentStream = null;
        contentStreamOverwrite = false;
        contentStreamUpdated = false;
    }
}
