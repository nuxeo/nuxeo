/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Estelle Giuly <egiuly@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.download;

import java.util.Arrays;

import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.runtime.api.Framework;

/**
 * This class exposes information of a blob given its download path. For instance, it parses the download path
 * "default/3727ef6b-cf8c-4f27-ab2c-79de0171a2c8/files:files/0/file/image.png" into:
 *
 * <pre>
 * {
 *   "repository": "default",
 *   "docId": "3727ef6b-cf8c-4f27-ab2c-79de0171a2c8",
 *   "xpath": "files:files/0/file",
 *   "filename": "image.png"
 * }
 * </pre>
 *
 * @since 9.1
 */
public class DownloadBlobInfo {
    protected final String repository;

    protected final String docId;

    protected final String xpath;

    protected final String filename;

    public DownloadBlobInfo(String downloadPath) {
        String[] parts = downloadPath.split("/");
        int length = parts.length;
        if (length < 2) {
            throw new IllegalArgumentException("The path \"" + downloadPath + "\" is not used to download blobs.");
        }
        repository = parts[0];
        docId = parts[1];
        if (length == 2) {
            xpath = null;
            filename = null;
        } else if (length == 3) {
            xpath = parts[2];
            filename = null;
        } else {
            String rest = String.join("/", Arrays.asList(parts).subList(2, length));
            if (Framework.getService(SchemaManager.class).getField(rest) != null) {
                xpath = rest;
                filename = null;
            } else {
                xpath = String.join("/", Arrays.asList(parts).subList(2, length - 1));
                filename = parts[length - 1];
            }
        }
    }
}
