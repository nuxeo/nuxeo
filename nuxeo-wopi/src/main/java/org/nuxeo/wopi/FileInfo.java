/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.wopi;

import java.util.Arrays;

import org.apache.commons.codec.binary.Base64;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Given a WOPI file id, this class extracts information about a blob: a repository name, a doc id and the blob xpath.
 * <p>
 * A WOPI file id is a base64 encoded string formatted as {@code repositoryName/docId/xpath}.
 *
 * @since 10.3
 */
public class FileInfo {

    public final String repositoryName;

    public final String docId;

    public final String xpath;

    /**
     * Returns a WOPI file id given a {@code repositoryName}, {@code docId} and a blob {@code xpath}.
     */
    public static String computeFileId(String repositoryName, String docId, String xpath) {
        return Base64.encodeBase64String(String.join("/", repositoryName, docId, xpath).getBytes());
    }

    /**
     * Returns a WOPI file id given a {@code doc}, and a blob {@code xpath}.
     */
    public static String computeFileId(DocumentModel doc, String xpath) {
        return computeFileId(doc.getRepositoryName(), doc.getId(), xpath);
    }

    public FileInfo(String fileId) {
        String str = new String(Base64.decodeBase64(fileId));
        String[] parts = str.split("/");
        repositoryName = parts[0];
        docId = parts[1];
        xpath = String.join("/", Arrays.asList(parts).subList(2, parts.length));
    }
}
