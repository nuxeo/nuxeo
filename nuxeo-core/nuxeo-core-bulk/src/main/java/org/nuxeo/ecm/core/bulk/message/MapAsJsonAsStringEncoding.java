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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.bulk.message;

import static java.util.Collections.emptyMap;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.Encoder;
import org.apache.avro.reflect.CustomEncoding;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This {@link CustomEncoding} encodes/decodes {@link Map}&lt;{@link String}, {@link Serializable}&gt; to a JSON
 * {@link String} using Jackson before encoding it in Avro format.
 *
 * @since 10.3
 */
public class MapAsJsonAsStringEncoding extends CustomEncoding<Map<String, Serializable>> {

    protected static final int NULL_SCHEMA_INDEX = 0;

    protected static final int STRING_SCHEMA_INDEX = 1;

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    public MapAsJsonAsStringEncoding() {
        List<Schema> union = Arrays.asList(Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.STRING));
        union.get(1).addProp("CustomEncoding", "MapAsJsonAsStringEncoding");
        schema = Schema.createUnion(union);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void write(Object o, Encoder encoder) throws IOException {
        if (o == null || ((Map<String, Serializable>) o).isEmpty()) {
            // treat empty map as null to save some bytes
            // encode the position of the data in the union
            encoder.writeIndex(NULL_SCHEMA_INDEX);
            encoder.writeNull();
        } else {
            // encode the position of the data in the union
            encoder.writeIndex(STRING_SCHEMA_INDEX);
            String mapAsJson = MAPPER.writeValueAsString(o);
            encoder.writeString(mapAsJson);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Map<String, Serializable> read(Object o, Decoder decoder) throws IOException {
        int index = decoder.readIndex();
        if (index == NULL_SCHEMA_INDEX) {
            decoder.readNull();
            return emptyMap();
        } else if (index == STRING_SCHEMA_INDEX) {
            String mapAsJson = decoder.readString();
            return new ObjectMapper().readValue(mapAsJson, Map.class);
        } else {
            throw new IOException("Unable to read Map as Json as String, index=" + index + " is unknown");
        }
    }
}
