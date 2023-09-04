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

package org.nuxeo.ecm.automation.core.util;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.platform.web.common.RequestContext;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Blob decoder resolving a Blob from a given download URL.
 * <p>
 * Format is:
 *
 * <pre>
 * {
 *     "data": "http://localhost:8080/nuxeo/nxfile/REPOSITORY/DOC_ID/file:content"
 * }
 * </pre>
 *
 * @since 10.3
 */
public class JSONObjectBlobDecoder implements JSONBlobDecoder {

    private static final Logger log = LogManager.getLogger(JSONObjectBlobDecoder.class);

    public static final String DATA_FIELD_NAME = "data";

    // @since 2021.43
    public static final String BLOB_URL_FIELD_NAME = "blobUrl";

    @Override
    public Blob getBlobFromJSON(ObjectNode jsonObject) {
        if (!jsonObject.has(DATA_FIELD_NAME)) {
            return null;
        }

        Blob blob = getBlobFromField(jsonObject, DATA_FIELD_NAME);
        if (blob == null) {
            blob = getBlobFromField(jsonObject, BLOB_URL_FIELD_NAME);
        }

        return blob;
    }

    protected Blob getBlobFromField(ObjectNode jsonObject, String fieldName) {
        JsonNode jsonNode = jsonObject.get(fieldName);
        if (jsonNode == null) {
            return null;
        }

        String blobUrl = jsonNode.textValue();
        return blobUrl != null ? getBlobFromURL(blobUrl) : null;
    }

    protected Blob getBlobFromURL(String url) {
        if (!url.startsWith("http")) {
            return null;
        }

        RequestContext activeContext = RequestContext.getActiveContext();
        if (activeContext == null) {
            return null;
        }

        String baseURL = VirtualHostHelper.getBaseURL(activeContext.getRequest());
        String path = url.replace(baseURL, "");
        if (path.startsWith("http")) {
            // the data URL does not target the current server, do nothing
            return null;
        }

        Blob blob = Framework.getService(DownloadService.class).resolveBlobFromDownloadUrl(path);
        if (blob == null) {
            // document does not exist / no READ permission / no blob
            log.debug("No Blob found for: {}", url);
            throw new NuxeoException(HttpServletResponse.SC_BAD_REQUEST);
        }
        return blob;
    }

}
