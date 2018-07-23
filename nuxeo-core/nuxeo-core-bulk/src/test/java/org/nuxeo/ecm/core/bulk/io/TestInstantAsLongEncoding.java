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
package org.nuxeo.ecm.core.bulk.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.Instant;

import javax.inject.Inject;

import org.apache.avro.reflect.AvroEncode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.runtime.codec.CodecService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features(CoreBulkFeature.class)
public class TestInstantAsLongEncoding {

    @Inject
    private CodecService codecService;

    @Test
    public void testInstantSerializationWithAvro() {
        BeanWithInstant bean = new BeanWithInstant();

        Codec<BeanWithInstant> codec = codecService.getCodec("avro", BeanWithInstant.class);
        byte[] bytes = codec.encode(bean);
        BeanWithInstant actualBean = codec.decode(bytes);

        assertNull(actualBean.getNullInstant());
        // assert milliseconds as we don't serialize nanoseconds
        assertEquals(bean.getNowInstant().toEpochMilli(), actualBean.getNowInstant().toEpochMilli());
    }

    public static class BeanWithInstant {

        @AvroEncode(using = InstantAsLongEncoding.class)
        protected final Instant nullInstant = null;

        @AvroEncode(using = InstantAsLongEncoding.class)
        protected final Instant nowInstant = Instant.now();

        public Instant getNullInstant() {
            return nullInstant;
        }

        public Instant getNowInstant() {
            return nowInstant;
        }
    }

}
