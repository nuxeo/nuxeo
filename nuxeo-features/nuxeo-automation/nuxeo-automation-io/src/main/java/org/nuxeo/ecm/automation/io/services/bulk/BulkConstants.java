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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.automation.io.services.bulk;

/**
 * Bulk constants for serialization.
 *
 * @since 10.2
 */
public class BulkConstants {

    // BulkStatus part

    public static final String BULK_ENTITY_TYPE = "bulk";

    public static final String BULK_ID = "id";

    public static final String BULK_STATE = "state";

    public static final String BULK_SUBMIT = "submit";

    public static final String BULK_COMMAND = "command";

    public static final String BULK_COUNT = "count";

    // BulkCommand part

    public static final String COMMAND_ENTITY_TYPE = "command";

    public static final String COMMAND_USERNAME = "username";

    public static final String COMMAND_REPOSITORY = "repository";

    public static final String COMMAND_QUERY = "query";

    public static final String COMMAND_OPERATION = "operation";

    private BulkConstants() {
        // constants class
    }
}
