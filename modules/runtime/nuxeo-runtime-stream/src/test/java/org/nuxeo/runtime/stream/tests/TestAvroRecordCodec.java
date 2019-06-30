/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.EnumSet;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;
import org.nuxeo.runtime.stream.RuntimeStreamFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 11.4
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeStreamFeature.class)
@Deploy("org.nuxeo.runtime.stream")
@Deploy("org.nuxeo.runtime.stream:test-codec-contrib.xml")
public class TestAvroRecordCodec {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder(new File(FeaturesRunner.getBuildDirectory()));

    public static final String SCHEMA_REGISTRY_URL_PROP = "confluent.schema_registry.urls";

    public static final String SCHEMA_REGISTRY_URL_DEFAULT = "http://localhost:8081";

    public static final String CONFLUENT_PROP = "confluent";

    protected final Codec<TestMessage> messageCodec = Framework.getService(CodecService.class).getCodec("avroBinary", TestMessage.class);

    public static void assumeConfluentRegistryEnabled() {
        Assume.assumeTrue("Skip Confluent tests", "true".equals(System.getProperty(CONFLUENT_PROP)));
    }

    public static String getConfluentRegistryUrls() {
        String url = System.getProperty(SCHEMA_REGISTRY_URL_PROP, SCHEMA_REGISTRY_URL_DEFAULT);
        if (url == null || url.isEmpty()) {
            url = SCHEMA_REGISTRY_URL_DEFAULT;
        }
        return url;
    }

    @Test
    public void testAvroRecordCodec() throws Exception {
        assumeConfluentRegistryEnabled();

        Record src = getRecord();
        Codec<Record> codec = Framework.getService(CodecService.class).getCodec("testMessageFlat", Record.class);

        byte[] data = codec.encode(src);
        // System.out.println("msg : " + src.getData().length + " " + overview(src.getData()));
        // System.out.println("rec+msg: " + data.length + " " + overview(data));
        Record dest = codec.decode(data);
        testCodec(src, codec);
    }

    protected Record testCodec(Record src, Codec<Record> codec) {
        byte[] data = codec.encode(src);
        Record dest = codec.decode(data);

        assertEquals(src.getKey(), dest.getKey());
        assertEquals(src.getWatermark(), dest.getWatermark());
        assertEquals(src, dest);

        TestMessage msgSrc = messageCodec.decode(src.getData());
        TestMessage msgDest = messageCodec.decode(dest.getData());

        assertEquals(msgSrc, msgDest);

        byte[] data2 = codec.encode(dest);
        Record dest2 = codec.decode(data2);
        assertEquals(src, dest2);
        assertEquals(String.format("%s\n%s", overview(data), overview(data2)), data.length, data2.length);
        // System.out.println(String.format("Codec: %s, size: %d", codec.getClass().getSimpleName(), data.length));
        return dest2;
    }

    protected Record getRecord() {
        TestMessage message = new TestMessage("foo", 1234L, true);
        Record src = Record.of("key", messageCodec.encode(message));
        src.setFlags(EnumSet.of(Record.Flag.COMMIT, Record.Flag.USER2, Record.Flag.DEFAULT));
        return src;
    }

    protected String overview(byte[] data) {
        String overview;
        overview = new String(data, UTF_8);
        return overview;
    }

}
