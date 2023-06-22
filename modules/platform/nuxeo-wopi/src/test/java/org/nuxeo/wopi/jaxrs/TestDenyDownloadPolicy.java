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
 *     Thomas Roger
 */
package org.nuxeo.wopi.jaxrs;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.wopi.Headers.OVERRIDE;

import java.util.Map;

import org.junit.Test;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.wopi.Operation;

/**
 * @since 2021.40
 */
@Deploy("org.nuxeo.wopi:OSGI-INF/test-download-permissions-contrib.xml")
public class TestDenyDownloadPolicy extends AbstractTestFilesEndpoint {

    // NXP-31828
    @Test
    public void testWithDenyDownloadPolicy() {
        // CheckFileInfo
        // cannot download the blob, even if john has write access
        try (CloseableClientResponse response = get(johnToken, blobDocFileId)) {
            assertEquals(404, response.getStatus());
        }
        // cannot download the blob, even if joe has read access
        try (CloseableClientResponse response = get(joeToken, blobDocFileId)) {
            assertEquals(404, response.getStatus());
        }

        // GetFile
        // cannot download the blob, even if john has write access
        try (CloseableClientResponse response = get(johnToken, blobDocFileId, CONTENTS_PATH)) {
            assertEquals(404, response.getStatus());
        }
        // cannot download the blob, even if joe has read access
        try (CloseableClientResponse response = get(joeToken, blobDocFileId, CONTENTS_PATH)) {
            assertEquals(404, response.getStatus());
        }

        // PutFile
        String data = "new content";
        Map<String, String> headers = Map.of(OVERRIDE, Operation.PUT.name());
        // cannot download the blob, so cannot use the PutFile WOPI operation even if john has write access
        try (CloseableClientResponse response = post(johnToken, data, headers, zeroLengthBlobDocFileId,
                CONTENTS_PATH)) {
            assertEquals(404, response.getStatus());
        }
    }

}
