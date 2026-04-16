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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.restapi.test;

import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_ERROR_COUNT;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_ERROR_MESSAGE;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_HAS_ERROR;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_PROCESSED;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_PROCESSING_MILLIS;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_SKIP_COUNT;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_STATE;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_TOTAL;

import java.io.IOException;
import java.time.Instant;

import org.nuxeo.common.function.ThrowableRunnable;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 2025.19
 */
public class BulkStatusJsonAssert {

    protected final JsonAssert jsonAssert;

    protected BulkStatusJsonAssert(JsonAssert jsonAssert) {
        this.jsonAssert = jsonAssert;
    }

    public static BulkStatusJsonAssert on(JsonNode node) {
        try {
            return new BulkStatusJsonAssert(JsonAssert.on(node.toString()));
        } catch (IOException e) {
            throw new AssertionError("Failed to parse JSON", e);
        }
    }

    public BulkStatusJsonAssert isCompleted() {
        return safeEval(() -> {
            jsonAssert.has(STATUS_STATE).isText().isEquals(BulkStatus.State.COMPLETED.name());
            Instant completed = Instant.parse(jsonAssert.has("completed").isText().getNode().asText());
            assertTrue(completed.isBefore(Instant.now()));
            jsonAssert.has(STATUS_PROCESSING_MILLIS).isInt();
        });
    }

    public BulkStatusJsonAssert hasProcessed(long expected) {
        return safeEval(() -> jsonAssert.has(STATUS_PROCESSED).isInt().isEquals(expected));
    }

    public BulkStatusJsonAssert hasTotal(long expected) {
        return safeEval(() -> jsonAssert.has(STATUS_TOTAL).isInt().isEquals(expected));
    }

    public BulkStatusJsonAssert hasError() {
        return safeEval(() -> jsonAssert.has(STATUS_HAS_ERROR).isBool().isEquals(true));
    }

    public BulkStatusJsonAssert hasError(long errorCount) {
        return safeEval(() -> {
            jsonAssert.has(STATUS_HAS_ERROR).isBool().isEquals(true);
            jsonAssert.has(STATUS_ERROR_COUNT).isInt().isEquals(errorCount);
        });
    }

    public BulkStatusJsonAssert hasError(String errorMessage) {
        return safeEval(() -> {
            jsonAssert.has(STATUS_HAS_ERROR).isBool().isEquals(true);
            jsonAssert.has(STATUS_ERROR_MESSAGE).isText().isEquals(errorMessage);
        });
    }

    public BulkStatusJsonAssert hasError(long errorCount, String errorMessage) {
        return safeEval(() -> {
            jsonAssert.has(STATUS_HAS_ERROR).isBool().isEquals(true);
            jsonAssert.has(STATUS_ERROR_COUNT).isInt().isEquals(errorCount);
            jsonAssert.has(STATUS_ERROR_MESSAGE).isText().isEquals(errorMessage);
        });
    }

    public BulkStatusJsonAssert hasNoError() {
        return safeEval(() -> {
            jsonAssert.has(STATUS_HAS_ERROR).isBool().isEquals(false);
            jsonAssert.has(STATUS_ERROR_COUNT).isInt().isEquals(0);
        });
    }

    public BulkStatusJsonAssert hasSkip(long expected) {
        return safeEval(() -> jsonAssert.has(STATUS_SKIP_COUNT).isInt().isEquals(expected));
    }

    protected BulkStatusJsonAssert safeEval(ThrowableRunnable<IOException> runnable) {
        try {
            runnable.run();
            return this;
        } catch (IOException e) {
            throw new AssertionError("Failed to parse JSON", e);
        }
    }
}
