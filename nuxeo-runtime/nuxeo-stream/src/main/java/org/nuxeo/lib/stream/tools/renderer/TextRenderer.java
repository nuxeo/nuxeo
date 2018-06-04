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
public class TextRenderer extends Renderer {

    @Override
    public void accept(LogRecord<Record> record) {
        try {
            Record rec = record.message();
            System.out.println(
                String.format("|%s|%s|%s|%d|%s|%s|", record.offset(), rec.getKey(), watermarkString(rec.getWatermark()),
                        rec.getData().length, rec.getFlags(), binaryString(rec.getData())));
        } catch (ClassCastException e) {
            // Try to render something else than a stream Record
            System.out.println(String.format("%s", record.message()));
        }
    }

    @Override
    public void header() {
        System.out.println(
                "| offset | key | watermark | length | flag | data |\n" + "| --- | ---: | --- | ---: | --- | --- |");
    }

    @Override
    public void footer() {
        System.out.println("");
    }
}
