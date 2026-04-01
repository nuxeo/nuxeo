/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.automation.server.test.operations;

import org.nuxeo.common.utils.DurationUtils;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;

/**
 * @since 2025.18
 */
@Operation(id = GenerateBlobOperation.ID)
public class GenerateBlobOperation {

    public static final String ID = "GenerateBlob";

    @Param(name = "delay", required = false)
    protected String delay = "1s";

    @Param(name = "content", required = false)
    protected String content = "Hello World!";

    @OperationMethod
    public Blob run() {
        try {
            Thread.sleep(DurationUtils.parse(delay).toMillis());
            return Blobs.createBlob(content, "text/plain");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

}
