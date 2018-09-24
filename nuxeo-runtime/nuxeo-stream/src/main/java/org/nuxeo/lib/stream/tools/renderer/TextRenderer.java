/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.lib.stream.tools.renderer;

import java.nio.file.Paths;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.codec.FileAvroSchemaStore;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogRecord;

/**
 * @since 9.3
 */
public class TextRenderer extends Renderer {
    private static final Log log = LogFactory.getLog(TextRenderer.class);

    protected final FileAvroSchemaStore schemaStore;

    public TextRenderer(String avroSchemaStorePath) {
        if (avroSchemaStorePath != null) {
            schemaStore = new FileAvroSchemaStore(Paths.get(avroSchemaStorePath));
        } else {
            schemaStore = null;
        }
    }

    @Override
    public void accept(LogRecord<Record> record) {
        try {
            Record rec = record.message();
            log.info(String.format("|%s|%s|%s|%s|%d|%s|", record.offset(), watermarkString(rec.getWatermark()),
                    rec.getFlags(), rec.getKey(), rec.getData().length, tryToRenderAvroData(schemaStore, rec)));
        } catch (ClassCastException e) {
            // Try to render something else than a stream Record
            log.info(String.format("%s", record.message()));
        }
    }

    @Override
    public void header() {
        log.info("| offset | watermark | flag | key | length | data |\n" + "| --- | --- | --- | --- | ---: | --- |");
    }

    @Override
    public void footer() {
        log.info("");
    }
}
