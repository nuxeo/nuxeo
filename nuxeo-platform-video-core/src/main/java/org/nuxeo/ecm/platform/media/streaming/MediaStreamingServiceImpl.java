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
 *     "<a href=\"mailto:bjalon@nuxeo.com\">Benjamin JALON</a>"
 */
package org.nuxeo.ecm.platform.media.streaming;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.repository.RepositoryDescriptor;
import org.nuxeo.ecm.core.repository.RepositoryManager;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.storage.sql.DefaultBinaryManager;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLBlob;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLRepository;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class MediaStreamingServiceImpl extends DefaultComponent implements
        MediaStreamingService {

    protected static Log log = LogFactory.getLog(MediaStreamingServiceImpl.class);

    protected boolean isServiceActivated = false;

    protected String streamingServerBaseURL = null;

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (extensionPoint.equals("video")) {
            MediaStreamingActivationDescriptor config = (MediaStreamingActivationDescriptor) contribution;
            isServiceActivated = config.activated;
            streamingServerBaseURL = config.streamServerBaseURL;
        }
    }

    public boolean isServiceActivated() {
        return isServiceActivated;
    }

    protected void setServiceActivated(boolean isServiceActivated) {
        this.isServiceActivated = isServiceActivated;
    }

    public String getStreamingServerBaseURL() {
        return streamingServerBaseURL;
    }

    protected void setStreamingServerBaseURL(String streamingServerBaseURL) {
        this.streamingServerBaseURL = streamingServerBaseURL;
    }

    protected final Map<String, DefaultBinaryManager> binaryManagers = new HashMap<String, DefaultBinaryManager>();

    protected Map<String, String> blobStoreFSPath = new HashMap<String, String>();

    public String getStreamURLFromDocumentModel(DocumentModel mediaDoc)
            throws ClientException {

        if (!mediaDoc.hasSchema(MediaStreamingConstants.STREAM_MEDIA_SCHEMA)) {
            log.error("DocId " + mediaDoc.getId()
                    + " is not a streamable document");
            throw new ClientException("Can't get stream from "
                    + mediaDoc.getType() + " document type. Schema \""
                    + MediaStreamingConstants.STREAM_MEDIA_SCHEMA
                    + "\" not present");
        }

        Blob blob = (Blob) mediaDoc.getPropertyValue(MediaStreamingConstants.STREAM_MEDIA_FIELD);
        if ((!isServiceActivated()) || blob == null) {
            return null;
        }

        String repositoryName = mediaDoc.getRepositoryName();
        DefaultBinaryManager binaryManager = getBinaryManager(repositoryName);

        if (!(blob instanceof SQLBlob)) {
            throw new ClientException(
                    "Media stream blob must stored in a SQLBlob");
        }

        SQLBlob sqlBlob = (SQLBlob) blob;

        File file = binaryManager.getFileForDigest(
                sqlBlob.getBinary().getDigest(), false);

        String absolutePath = file.getAbsolutePath();
        String blobStorageDir = getBlobStorageDirRootPath(repositoryName);
        if (!absolutePath.startsWith(blobStorageDir)) {
            throw new ClientException(
                    "Media Stream not stored in the blob store : "
                            + absolutePath);
        }

        StringBuffer url = new StringBuffer(getStreamingServerBaseURL());
        url.append(absolutePath.substring(getBlobStorageDirRootPath(
                repositoryName).length()).replace("\\", "/"));

        return url.toString();

    }

    protected DefaultBinaryManager getBinaryManager(String repositoryName)
            throws ClientException {
        if (!binaryManagers.containsKey(repositoryName)) {
            RepositoryService repositoryService = (RepositoryService) Framework.getRuntime().getComponent(
                    RepositoryService.NAME);
            RepositoryManager repositoryManager = repositoryService.getRepositoryManager();
            RepositoryDescriptor descriptor = repositoryManager.getDescriptor(repositoryName);
            try {
                DefaultBinaryManager binaryManager = new DefaultBinaryManager();
                binaryManager.initialize(SQLRepository.getDescriptor(descriptor));
                binaryManagers.put(repositoryName, binaryManager);
            } catch (IOException e) {
                throw new ClientException(e);
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }

        return binaryManagers.get(repositoryName);
    }

    protected String getBlobStorageDirRootPath(String repositoryName)
            throws ClientException {
        if (!blobStoreFSPath.containsKey(repositoryName)) {
            blobStoreFSPath.put(
                    repositoryName,
                    getBinaryManager(repositoryName).getStorageDir().getAbsolutePath());
        }
        return blobStoreFSPath.get(repositoryName);
    }

    public boolean isStreamableMedia(DocumentModel doc) {
        return doc.hasSchema(MediaStreamingConstants.STREAM_MEDIA_SCHEMA);
    }

}
