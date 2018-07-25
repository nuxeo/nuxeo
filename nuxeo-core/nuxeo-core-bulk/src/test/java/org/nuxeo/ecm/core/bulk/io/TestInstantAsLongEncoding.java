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

import org.apache.avro.reflect.AvroEncode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features(CoreBulkFeature.class)
public class TestInstantAsLongEncoding {

    @Rule
    public final CodecTestRule<BeanWithInstant> codecRule = new CodecTestRule<>("avro", BeanWithInstant.class);

    @Test
    public void testNullInstant() {
        BeanWithInstant bean = new BeanWithInstant(null);
        BeanWithInstant actualBean = codecRule.encodeDecode(bean);

        assertNull(actualBean.getInstant());
    }

    @Test
    public void testNowInstant() {
        BeanWithInstant bean = new BeanWithInstant(Instant.now());
        BeanWithInstant actualBean = codecRule.encodeDecode(bean);

        // assert milliseconds as we don't serialize nanoseconds
        assertEquals(bean.getInstant().toEpochMilli(), actualBean.getInstant().toEpochMilli());
    }

    public static class BeanWithInstant {

        @AvroEncode(using = InstantAsLongEncoding.class)
        protected Instant instant;

        public BeanWithInstant() {
            // for Avro
        }

        public BeanWithInstant(Instant instant) {
            this.instant = instant;
        }

        public Instant getInstant() {
            return instant;
        }
    }

}
