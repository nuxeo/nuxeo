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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.function.Consumer;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.message.BinaryMessageDecoder;
import org.nuxeo.lib.stream.codec.AvroSchemaStore;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Watermark;
import org.nuxeo.lib.stream.log.LogRecord;

/**
 * @since 9.3
 */
public abstract class Renderer implements Consumer<LogRecord<Record>> {

    public abstract void header();

    public abstract void footer();

    public static final byte[] AVRO_MESSAGE_V1_HEADER = new byte[] { (byte) 0xC3, (byte) 0x01 };

    protected String watermarkString(long watermark) {
        if (watermark == 0) {
            return "0";
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Watermark wm = Watermark.ofValue(watermark);
        return String.format("%s:%d%s", dateFormat.format(new Date(wm.getTimestamp())), wm.getSequence(),
                wm.isCompleted() ? " completed" : "");
    }

    protected String tryToRenderAvroData(AvroSchemaStore store, Record record) {
        String errorMessage;
        try {
            return renderAvroMessage(store, record);
        } catch (IllegalArgumentException e) {
            errorMessage = "";
        } catch (IllegalStateException e) {
            errorMessage = e.getMessage() + " data: ";
        }
        return errorMessage + record.dataOverview(256);

    }
    protected String renderAvroMessage(AvroSchemaStore store, Record record) {
        if (store == null || !isAvroMessage(record.getData())) {
            throw new IllegalArgumentException("Not avro encoded");
        }
        long fp = getFingerPrint(record.getData());
        Schema schema = store.findByFingerprint(fp);
        if (schema == null) {
            throw new IllegalStateException(String.format("Not found schema: 0x%08X", fp));
        }
        GenericData genericData = new GenericData();
        BinaryMessageDecoder<GenericRecord> decoder = new BinaryMessageDecoder<>(genericData, schema);
        try {
            GenericRecord avroRecord = decoder.decode(record.getData(), null);
            return avroRecord.toString();
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Error: %s decoding with schema: 0x%08X", e.getMessage(), fp));
        }
    }

    protected long getFingerPrint(byte[] data) {
        byte[] fingerPrintBytes = Arrays.copyOfRange(data, 2, 10);
        return ByteBuffer.wrap(fingerPrintBytes).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    protected boolean isAvroMessage(byte[] data) {
        if (data.length >= 10 && data[0] == AVRO_MESSAGE_V1_HEADER[0] && data[1] == AVRO_MESSAGE_V1_HEADER[1]) {
            return true;
        }
        return false;
    }
}
