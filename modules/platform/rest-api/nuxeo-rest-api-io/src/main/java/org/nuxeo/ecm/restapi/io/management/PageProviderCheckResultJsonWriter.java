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
package org.nuxeo.ecm.restapi.io.management;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import org.apache.commons.lang3.ClassUtils;
import org.nuxeo.common.function.ThrowableBiConsumer;
import org.nuxeo.common.utils.DurationUtils;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.query.api.PageProviderCheckResult;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 2025.19
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class PageProviderCheckResultJsonWriter extends AbstractJsonWriter<PageProviderCheckResult> {

    @Override
    public void write(PageProviderCheckResult entity, JsonGenerator jg) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("pageProvider", entity.pageProvider());

        jg.writeArrayFieldStart("orders");
        for (var order : entity.orders()) {
            jg.writeString(order.getSortColumn() + ' ' + (order.getSortAscending() ? "ASC" : "DESC"));
        }
        jg.writeEndArray();

        jg.writeObjectFieldStart("executions");
        entity.executions().forEach(ThrowableBiConsumer.asBiConsumer((name, execution) -> {
            jg.writeFieldName(name);
            write(execution, jg);
        }));
        jg.writeEndObject();

        jg.writeEndObject();
    }

    protected void write(PageProviderCheckResult.Execution execution, JsonGenerator jg) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("duration", DurationUtils.format(execution.duration()));
        jg.writeNumberField("resultsCount", execution.resultsCount());
        jg.writeNumberField("resultsCountLimit", execution.resultsCountLimit());

        jg.writeArrayFieldStart("results");
        for (var result : execution.results()) {
            if (result == null) {
                jg.writeNull();
            } else if (ClassUtils.isPrimitiveOrWrapper(result.getClass()) || result instanceof String) {
                jg.writeObject(result);
            } else {
                writeEntity(result, jg);
            }
        }
        jg.writeEndArray();

        jg.writeEndObject();
    }
}
