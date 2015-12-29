/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Kevin Leturc
 */
package org.nuxeo.ecm.liveconnect.box;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

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
}
