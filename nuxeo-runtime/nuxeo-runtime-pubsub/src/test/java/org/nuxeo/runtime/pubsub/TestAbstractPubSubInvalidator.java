/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.runtime.pubsub;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.nuxeo.runtime.mockito.MockitoFeature;
import org.nuxeo.runtime.mockito.RuntimeService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, MockitoFeature.class })
public class TestAbstractPubSubInvalidator {

    @Mock
    @RuntimeService
    protected PubSubService pubSubService;

    @Before
    public void doBefore() throws Exception {
        doNothing().when(pubSubService).registerSubscriber(anyString(), any());
    }

    public static class DummyInvalidations implements SerializableAccumulableInvalidations {

        private static final long serialVersionUID = 1L;

        protected boolean inval;

        public void inval() {
            inval = true;
        }

        @Override
        public void add(SerializableAccumulableInvalidations o) {
            DummyInvalidations other = (DummyInvalidations) o;
            inval = inval || other.inval;
        }

        @Override
        public boolean isEmpty() {
            return !inval;
        }

        @Override
        public void serialize(OutputStream out) throws IOException {
            if (inval) {
                out.write('Y');
            }
        }

        public static DummyInvalidations deserialize(InputStream in) throws IOException {
            DummyInvalidations invals = new DummyInvalidations();
            if (in.read() != -1) {
                invals.inval();
            }
            return invals;
        }
    }

    public static class DummyInvalidator extends AbstractPubSubInvalidationsAccumulator<DummyInvalidations> {

        @Override
        public DummyInvalidations newInvalidations() {
            return new DummyInvalidations();
        }

        @Override
        public DummyInvalidations deserialize(InputStream in) throws IOException {
            return DummyInvalidations.deserialize(in);
        }
    }

    @Test
    public void testDummyInvalidations() throws IOException {
        DummyInvalidations invals = new DummyInvalidations();
        DummyInvalidations invals2 = new DummyInvalidations();

        assertTrue(invals.isEmpty());
        assertTrue(roundTrip(invals).isEmpty());
        invals2.add(invals);
        assertTrue(invals2.isEmpty());

        invals.inval();

        assertFalse(invals.isEmpty());
        assertFalse(roundTrip(invals).isEmpty());
        invals2.add(invals);
        assertFalse(invals2.isEmpty());
    }

    protected DummyInvalidations roundTrip(DummyInvalidations invalidations) throws IOException {
        DummyInvalidator invalidator = new DummyInvalidator();
        try (ByteArrayOutputStream baout = new ByteArrayOutputStream()) {
            invalidations.serialize(baout);
            try (ByteArrayInputStream bain = new ByteArrayInputStream(baout.toByteArray())) {
                return invalidator.deserialize(bain);
            }
        }
    }

    @Test
    public void testScanDiscriminator() throws Exception {
        DummyInvalidator invalidator = new DummyInvalidator();
        invalidator.initialize("topic", "di");

        assertEquals(-1, invalidator.scanDiscriminator(null));
        assertEquals(-1, invalidator.scanDiscriminator("".getBytes(UTF_8)));
        assertEquals(-1, invalidator.scanDiscriminator("d".getBytes(UTF_8)));
        assertEquals(-1, invalidator.scanDiscriminator("di".getBytes(UTF_8)));
        assertEquals(-1, invalidator.scanDiscriminator("dis".getBytes(UTF_8)));
        assertEquals(-1, invalidator.scanDiscriminator("x".getBytes(UTF_8)));
        assertEquals(-1, invalidator.scanDiscriminator("xy".getBytes(UTF_8)));
        assertEquals(-1, invalidator.scanDiscriminator("xyz".getBytes(UTF_8)));

        assertEquals(1, invalidator.scanDiscriminator(":foo".getBytes(UTF_8)));
        assertEquals(1, invalidator.scanDiscriminator(":".getBytes(UTF_8)));
        assertEquals(2, invalidator.scanDiscriminator("x:foo".getBytes(UTF_8)));
        assertEquals(2, invalidator.scanDiscriminator("x:".getBytes(UTF_8)));
        assertEquals(3, invalidator.scanDiscriminator("xy:foo".getBytes(UTF_8)));
        assertEquals(3, invalidator.scanDiscriminator("xy:".getBytes(UTF_8)));
        assertEquals(4, invalidator.scanDiscriminator("xyz:foo".getBytes(UTF_8)));
        assertEquals(4, invalidator.scanDiscriminator("xyz:".getBytes(UTF_8)));

        assertEquals(2, invalidator.scanDiscriminator("d:foo".getBytes(UTF_8)));
        assertEquals(2, invalidator.scanDiscriminator("d:".getBytes(UTF_8)));
        assertEquals(-1, invalidator.scanDiscriminator("di:foo".getBytes(UTF_8)));
        assertEquals(-1, invalidator.scanDiscriminator("di:".getBytes(UTF_8)));
        assertEquals(4, invalidator.scanDiscriminator("dis:foo".getBytes(UTF_8)));
        assertEquals(4, invalidator.scanDiscriminator("dis:".getBytes(UTF_8)));

    }

    @Test
    public void testSubscriberDiscriminatorComparison() throws Exception {
        DummyInvalidator invalidator = new DummyInvalidator();
        invalidator.initialize("topic", "d");
        DummyInvalidations invals;

        // use different discriminator
        invalidator.subscriber("topic", "z:foo".getBytes(UTF_8));
        invals = invalidator.receiveInvalidations();
        assertFalse(invals.isEmpty());

        // use same discriminator
        invalidator.subscriber("topic", "d:foo".getBytes(UTF_8));
        invals = invalidator.receiveInvalidations();
        assertTrue(invals.isEmpty());
    }

}
