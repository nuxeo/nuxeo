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
 *     Funsho David
 */
package org.nuxeo.ecm.core.bulk.io;

/**
 * Bulk constants for serialization.
 *
 * @since 10.3
 */
public class BulkConstants {

    // BulkStatus

    public static final String STATUS_ENTITY_TYPE = "bulkStatus";

    public static final String STATUS_COMMAND_ID = "commandId";

    public static final String STATUS_STATE = "state";

    public static final String STATUS_SUBMIT_TIME = "submitted";

    public static final String STATUS_SCROLL_START = "scrollStart";

    public static final String STATUS_SCROLL_END = "scrollEnd";

    public static final String STATUS_COMPLETED_TIME = "completed";

    public static final String STATUS_TOTAL = "total";

    public static final String STATUS_ACTION = "action";

    public static final String STATUS_PROCESSED = "processed";

    public static final String STATUS_RESULT = "result";

    public static final String STATUS_USERNAME = "username";

    // BulkCommand

    public static final String COMMAND_ENTITY_TYPE = "bulkCommand";

    public static final String COMMAND_USERNAME = STATUS_USERNAME;

    public static final String COMMAND_REPOSITORY = "repository";

    public static final String COMMAND_QUERY = "query";

    public static final String COMMAND_ACTION = STATUS_ACTION;

    public static final String COMMAND_PARAMS = "params";

    public static final String COMMAND_BUCKET_SIZE = "bucket";

    public static final String COMMAND_BATCH_SIZE = "batch";

    private BulkConstants() {
        // constants class
    }

}
