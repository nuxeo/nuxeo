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
package org.nuxeo.runtime.stream.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.nuxeo.lib.stream.codec.NoCodec.NO_CODEC;

import java.util.EnumSet;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.codec.CodecService;
import org.nuxeo.runtime.stream.RuntimeStreamFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @since 10.2
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeStreamFeature.class)
public class TestCodecService {

    @Inject
    public CodecService service;

    @Test
    public void testService() {
        assertNotNull(service);
    }

    @Test
    public void testCodec() throws Exception {
        Record record = Record.of("key", "value".getBytes("UTF-8"));
        record.setFlags(EnumSet.of(Record.Flag.COMMIT, Record.Flag.POISON_PILL));

        Codec<Record> codec = service.getCodec("java", Record.class);
        checkCodec(record, codec, 90);
        codec = service.getCodec("avro", Record.class);
        checkCodec(record, codec, 30);
        codec = service.getCodec("avroBinary", Record.class);
        checkCodec(record, codec, 20);
        codec = service.getCodec("avroJson", Record.class);
        checkCodec(record, codec, 75);

        codec = service.getCodec("java", Record.class);
        checkCodec(record, codec, 90);

        codec = service.getCodec("legacy", Record.class);
        assertEquals(NO_CODEC, codec);
    }

    private void checkCodec(Record src, Codec<Record> codec, int expectedSize) {
        assertNotNull(codec);
        byte[] data = codec.encode(src);
        Record dest = codec.decode(data);
        assertEquals(src, dest);
        assertEquals(expectedSize, data.length);
    }
}
