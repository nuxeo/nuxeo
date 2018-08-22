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

import static java.lang.Math.min;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;

import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Watermark;
import org.nuxeo.lib.stream.log.LogRecord;

/**
 * @since 9.3
 */
public abstract class Renderer implements Consumer<LogRecord<Record>> {

    public abstract void header();

    public abstract void footer();

    protected String watermarkString(long watermark) {
        if (watermark == 0) {
            return "0";
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Watermark wm = Watermark.ofValue(watermark);
        return String.format("%s:%d%s", dateFormat.format(new Date(wm.getTimestamp())), wm.getSequence(),
                wm.isCompleted() ? " completed" : "");
    }

    protected String binaryString(byte[] data) {
        String overview = "";
        if (data != null) {
            overview += new String(data, StandardCharsets.UTF_8).substring(0, min(data.length, 512));
            overview = overview.replaceAll("[^\\x20-\\x7e]", ".");
        }
        return overview;
    }
}
