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

package org.nuxeo.ecm.restapi.test.versioning;

/**
 * Used for REST API v1 and v2 tests.
 *
 * @since 11.1
 */
public class DummyObject {

    public DummyObject() {
    }

    public DummyObject(String fieldV1, String fieldV2) {
        this.fieldV1 = fieldV1;
        this.fieldV2 = fieldV2;
    }

    public String fieldV1; // NOSONAR

    public String fieldV2; // NOSONAR
}
