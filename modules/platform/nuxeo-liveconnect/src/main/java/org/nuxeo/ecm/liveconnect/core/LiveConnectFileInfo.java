/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.liveconnect.core;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

/**
 * @since 8.1
 */
public class LiveConnectFileInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String user;

    private final String fileId;

    private final String revisionId;

    public LiveConnectFileInfo(String user, String fileId) {
        this(user, fileId, null);
    }

    public LiveConnectFileInfo(String user, String fileId, String revisionId) {
        this.user = Objects.requireNonNull(user);
        this.fileId = Objects.requireNonNull(fileId);
        this.revisionId = revisionId;
    }

    public String getUser() {
        return user;
    }

    public String getFileId() {
        return fileId;
    }

    public Optional<String> getRevisionId() {
        return Optional.ofNullable(revisionId);
    }

    @Override
    public String toString() {
        ToStringHelper helper = MoreObjects.toStringHelper(this);
        helper.add("user", user);
        helper.add("fileId", fileId);
        getRevisionId().ifPresent(rev -> helper.add("revisionId", rev));
        return helper.toString();
    }
}
