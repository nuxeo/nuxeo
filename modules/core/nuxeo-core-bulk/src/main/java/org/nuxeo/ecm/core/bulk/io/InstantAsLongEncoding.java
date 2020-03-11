/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 */

package org.nuxeo.ecm.core.bulk.io;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.Encoder;
import org.apache.avro.reflect.CustomEncoding;

/**
 * This {@link CustomEncoding} encodes/decodes {@link Instant} to a long (time in milliseconds) before encoding it in
 * Avro format.
 *
 * @since 10.3
 */
public class InstantAsLongEncoding extends CustomEncoding<Instant> {

    protected static final int NULL_SCHEMA_INDEX = 0;

    protected static final int LONG_SCHEMA_INDEX = 1;

    public InstantAsLongEncoding() {
        List<Schema> union = Arrays.asList(Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.LONG));
        union.get(1).addProp("CustomEncoding", "InstantAsLongEncoding");
        schema = Schema.createUnion(union);
    }

    @Override
    protected void write(Object o, Encoder encoder) throws IOException {
        if (o == null) {
            // encode the position of the data in the union
            encoder.writeIndex(NULL_SCHEMA_INDEX);
            encoder.writeNull();
        } else {
            // encode the position of the data in the union
            encoder.writeIndex(LONG_SCHEMA_INDEX);
            encoder.writeLong(((Instant) o).toEpochMilli());
        }
    }

    @Override
    protected Instant read(Object o, Decoder decoder) throws IOException {
        int index = decoder.readIndex();
        if (index == NULL_SCHEMA_INDEX) {
            decoder.readNull();
            return null;
        } else if (index == LONG_SCHEMA_INDEX) {
            return Instant.ofEpochMilli(decoder.readLong());
        } else {
            throw new IOException("Unable to read Instant as long, index=" + index + " is unknown");
        }
    }
}
