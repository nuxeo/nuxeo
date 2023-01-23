/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume RENARD
 */
package org.nuxeo.ecm.core.blob.stream;

import java.io.Serializable;

import org.apache.avro.reflect.Nullable;

/**
 * @since 2023
 */
public class BlobDomainEvent implements Serializable {

    private static final long serialVersionUID = -1510221168377796630L;

    public String source;

    public String event;

    // Required
    public String blobKey;

    // Required
    public String repository;

    /**
     * Typically null in test when the CoreFeature does cleanupSession.
     */
    @Nullable
    public String user;

    @Nullable
    public String docId;

    @Nullable
    public String xpath;

    @Nullable
    public String blobDigest;

    @Nullable
    public long blobLength;

    @Nullable
    public String mimeType;

    @Nullable
    public String fileName;

    public BlobDomainEvent() {
        // Required
    }

    @Override
    public String toString() {
        return "BlobDomainEvent{" + //
                "source='" + source + '\'' + //
                ", event='" + event + '\'' + //
                ", user='" + user + '\'' + //
                ", repository='" + repository + '\'' + //
                ", docId='" + docId + '\'' + //
                ", xpath='" + xpath + '\'' + //
                ", repository='" + repository + '\'' + //
                ", blobKey='" + blobKey + '\'' + //
                ", blobDigest='" + blobDigest + '\'' + //
                ", blobLength='" + blobLength + '\'' + //
                ", mimeType='" + mimeType + '\'' + //
                ", fileName='" + fileName + '\'' + //
                '}';
    }
}
