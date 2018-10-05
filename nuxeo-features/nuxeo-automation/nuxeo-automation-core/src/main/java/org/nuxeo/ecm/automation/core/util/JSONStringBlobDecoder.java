/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.automation.core.util;

import org.codehaus.jackson.node.ObjectNode;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;

/**
 * Very basic implementation of a Blob decoder Only usable for StringBlobs
 * <p>
 * Format is:
 *
 * <pre>
 * {
 *     "filename": "mydoc.txt",
 *     "name": "mydoc.txt", <-- if filename is null, read name
 *     "encoding": "UTF-8", <-- defaults to UTF-8
 *     "mime-type": "text/plain", <- defaults to text/plain
 *     "data": "my data",
 *     "content": "my data" <-- if data is not present, read content
 * }
 * </pre>
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.5
 */
public class JSONStringBlobDecoder implements JSONBlobDecoder {
    @Override
    public Blob getBlobFromJSON(ObjectNode jsonObject) {
        Blob blob;

        String filename = null;
        if (jsonObject.has("filename")) {
            filename = jsonObject.get("filename").getTextValue();
        }
        if (filename == null && jsonObject.has("name")) {
            filename = jsonObject.get("name").getTextValue();
        }
        String encoding = "UTF-8";
        if (jsonObject.has("encoding")) {
            encoding = jsonObject.get("encoding").getTextValue();
        }

        String mimetype = "text/plain";
        if (jsonObject.has("mime-type")) {
            mimetype = jsonObject.get("mime-type").getTextValue();
        }
        String data = null;
        if (jsonObject.has("data")) {
            data = jsonObject.get("data").getTextValue();
            // try to avoid the bug NXP-18488: data contains the blob url
            // and must not be recognized as a new blob content
            if (data.startsWith("http")) {
                data = null;
            }
        } else if (jsonObject.has("content")) {
            data = jsonObject.get("content").getTextValue();
        }
        if (data == null) {
            return null;
        } else {
            blob = Blobs.createBlob(data, mimetype, encoding, filename);
        }
        return blob;
    }
}
