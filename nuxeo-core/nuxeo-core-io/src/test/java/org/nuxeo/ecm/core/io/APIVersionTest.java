/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.core.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * @since 11.1
 */
public class APIVersionTest {

    @Test
    public void testLatestVersion() {
        APIVersion expectedLatest = APIVersion.VALID_VERSIONS.values()
                                                             .stream()
                                                             .max(APIVersion.COMPARATOR)
                                                             .orElseThrow();
        assertEquals(expectedLatest, APIVersion.latest());
    }

    @Test
    public void testInvalidAPIVersions() {
        testInvalidAPIVersion(0);
        testInvalidAPIVersion(-1);
        testInvalidAPIVersion(1000);
    }

    protected void testInvalidAPIVersion(int apiVersion) {
        try {
            APIVersion.of(apiVersion);
            fail(String.format("%s is not a valid REST API version", apiVersion));
        } catch (NuxeoException e) {
            String expectedMessage = String.format("%s is not part of the valid versions: %s", apiVersion,
                    APIVersion.VALID_VERSIONS.keySet());
            assertEquals(expectedMessage, e.getMessage());
        }
    }

    @Test
    public void testComparingAPIVersions() {
        APIVersion version = APIVersion.of(11);
        assertTrue(version.eq(APIVersion.V11));
        assertTrue(version.neq(APIVersion.V1));
        assertTrue(version.gt(APIVersion.V1));
        assertTrue(version.gte(APIVersion.V1));
        assertTrue(version.gte(APIVersion.V11));

        version = APIVersion.of(1);
        assertTrue(version.eq(APIVersion.V1));
        assertTrue(version.neq(APIVersion.V11));
        assertTrue(version.lt(APIVersion.V11));
        assertTrue(version.lte(APIVersion.V11));
        assertTrue(version.lte(APIVersion.V1));
    }
}
