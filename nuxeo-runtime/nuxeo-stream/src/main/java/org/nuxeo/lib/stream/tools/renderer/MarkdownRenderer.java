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
public class MarkdownRenderer extends Renderer {
    private static final Log log = LogFactory.getLog(Renderer.class);

    protected static final String MD_DATA = "```";

    protected final FileAvroSchemaStore schemaStore;

    public MarkdownRenderer(String avroSchemaStorePath) {
        if (avroSchemaStorePath != null) {
            schemaStore = new FileAvroSchemaStore(Paths.get(avroSchemaStorePath));
        } else {
            schemaStore = null;
        }
    }

    @Override
    public void accept(LogRecord<Record> record) {
        Record rec = record.message();
        log.info(String.format("### %s: key: %s, wm: %s, len: %d, flag: %s", record.offset(), rec.getKey(),
                watermarkString(rec.getWatermark()), rec.getData().length, rec.getFlags()));
        log.info(MD_DATA + tryToRenderAvroData(schemaStore, rec) + MD_DATA);
    }

    @Override
    public void header() {
        log.info("## Records");
    }

    @Override
    public void footer() {
        log.info("");
    }
}
