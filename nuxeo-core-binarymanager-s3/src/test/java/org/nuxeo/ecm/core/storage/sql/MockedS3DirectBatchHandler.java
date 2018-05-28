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
 *     pierre
 */
package org.nuxeo.ecm.core.storage.sql;

/**
 * Mocked S3 Direct Upload handler to be tested.
 *
 * @since 10.2
 */
public class MockedS3DirectBatchHandler extends S3DirectBatchHandler {

    @Override
    protected long lowerThresholdToUseMultipartCopy() {
        return TestS3DirectBatchHandler.MULTIPART_THRESHOLD + 1;
    }

}
