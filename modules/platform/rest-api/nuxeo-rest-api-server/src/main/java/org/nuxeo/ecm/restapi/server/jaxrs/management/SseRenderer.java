/*
 * (C) Copyright 2022 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.restapi.server.jaxrs.management;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletResponse;

import org.nuxeo.lib.stream.codec.AvroSchemaStore;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Watermark;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.tools.renderer.Renderer;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.avro.AvroService;

/**
 * @since 2021.22
 */
public class SseRenderer extends Renderer implements Closeable  {

    protected final PrintWriter writer;

    protected final AvroSchemaStore schemaStore;

    protected final HttpServletResponse response;

    public SseRenderer(HttpServletResponse response) throws IOException {
        this.response = response;
        writer = response.getWriter();
        AvroService avroService = Framework.getService(AvroService.class);
        schemaStore = avroService.getSchemaStore();
        this.dataSize = 2_000_000;
    }

    @Override
    public void accept(LogRecord<Record> record) {
        try {
            Record rec = record.message();
            writeJson(String.format(
                    "{\"type\":\"record\",\"stream\":\"%s\",\"partition\":%d,\"offset\":%d,\"watermark\":\"%s\",\"key\":\"%s\",\"length\":%d,\"message\": %s}",
                    record.offset().partition().name().getUrn(), record.offset().partition().partition(),
                    record.offset().offset(), watermarkString(rec.getWatermark()), rec.getKey(), rec.getData().length,
                    asValidJson(tryToRenderAvroData(schemaStore, rec))));
        } catch (ClassCastException e) {
            // Try to render something else than a stream Record
            writeJson(String.format("{\"type\":\"recordBinary\",\"message\":\"%s\"}", record.message()));
        }
    }

    protected String asValidJson(String data) {
        if (data.startsWith("{") && data.endsWith("}")) {
            return data;
        }
        return "\"" + data.replaceAll("[\"'\\\\]", ".") + "\"";
    }

    public void flush() throws IOException {
        response.flushBuffer();
    }

    public void writeJson(String json) {
        writer.print("data: " + json + "\n\n");
    }

    @Override
    protected String watermarkString(long watermark) {
        if (watermark == 0) {
            return "NA";
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Watermark wm = Watermark.ofValue(watermark);
        return dateFormat.format(new Date(wm.getTimestamp()));
    }

    @Override
    public void header() {
    }

    @Override
    public void footer() {
    }

    @Override public void close() throws IOException {
        writer.close();
    }

}
