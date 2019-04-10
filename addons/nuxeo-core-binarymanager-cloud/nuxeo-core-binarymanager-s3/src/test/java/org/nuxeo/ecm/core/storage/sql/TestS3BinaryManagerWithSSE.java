/*
 * (C) Copyright 2019 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     pierre
 */
package org.nuxeo.ecm.core.storage.sql;

import static org.nuxeo.ecm.core.storage.sql.S3BinaryManager.SERVERSIDE_ENCRYPTION_PROPERTY;

import org.junit.BeforeClass;

/**
 * Tests S3BinaryManager with Server Side Encryption activated.
 *
 * @since 11.1
 */
public class TestS3BinaryManagerWithSSE extends TestS3BinaryManager {

    @BeforeClass
    public static void beforeClass() {
        TestS3BinaryManager.beforeClass();
        // activate server side encryption
        PROPERTIES.put(SERVERSIDE_ENCRYPTION_PROPERTY, Boolean.TRUE.toString());
    }

}
