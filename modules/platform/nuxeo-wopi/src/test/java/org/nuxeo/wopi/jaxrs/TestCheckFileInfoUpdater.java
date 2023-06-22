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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Deploy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 2021.40
 */
@Deploy("org.nuxeo.wopi:OSGI-INF/test-checkfileinfo-updater-contrib.xml")
public class TestCheckFileInfoUpdater extends AbstractTestFilesEndpoint {

    // NXP-31852
    @Test
    public void testCheckFileInfoUpdater() throws IOException {
        try (CloseableClientResponse response = get(johnToken, blobDocFileId)) {
            assertEquals(200, response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            Map<String, Serializable> map = mapper.convertValue(node, new TypeReference<>() {
            });
            assertTrue(map.values().stream().allMatch("foobar"::equals));
        }
    }

}
