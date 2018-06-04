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

import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogRecord;

/**
 * @since 9.3
 */
public class MarkdownRenderer extends Renderer {
    protected static final String MD_DATA = "```";

    @Override
    public void accept(LogRecord<Record> record) {
        Record rec = record.message();
        System.out.println(String.format("### %s: key: %s, wm: %s, len: %d, flag: %s", record.offset(), rec.getKey(),
                watermarkString(rec.getWatermark()), rec.getData().length, rec.getFlags()));
        System.out.println(MD_DATA + binaryString(rec.getData()) + MD_DATA);
    }

    @Override
    public void header() {
        System.out.println("## Records");
    }

    @Override
    public void footer() {
        System.out.println("");
    }
}
