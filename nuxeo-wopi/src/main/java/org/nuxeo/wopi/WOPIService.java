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

import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * WOPI Service.
 *
 * @since 10.3
 */
public interface WOPIService {

    /**
     * Returns @{code true} if WOPI is enabled, {@code false} otherwise.
     * <p>
     * The WOPI discovery XML file has been loaded.
     */
    boolean isEnabled();

    /**
     * Returns a list of {@link WOPIBlobInfo} for the document's blobs supported by WOPI.
     */
    List<WOPIBlobInfo> getWOPIBlobInfos(DocumentModel doc);

    /**
     * Returns the WOPI action url given a {@code blob} and an {@code action}.
     */
    String getActionURL(Blob blob, String action);

    /**
     * Verifies that the request originate from Office Online.
     */
    boolean verifyProofKey(String proofKeyHeader, String oldProofKeyHeader, String url, String accessToken,
            String timestampHeader);

}
