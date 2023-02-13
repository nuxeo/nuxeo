/*
 * (C) Copyright 2022 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.mongodb.transientstore;

import static com.mongodb.internal.connection.tlschannel.util.Util.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreProvider;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 2021.30
 */
@RunWith(FeaturesRunner.class)
@Features(TransientStoreMongoDBFeature.class)
public class TestMongoDBSpecificTransientStore {

    @Inject
    protected TransientStoreService tss;

    static class SValue implements Serializable {
        int i;

        String s;

        public SValue(int i, String s) {
            this.i = i;
            this.s = s;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            SValue sValue = (SValue) o;
            return i == sValue.i && Objects.equals(s, sValue.s);
        }

        @Override
        public int hashCode() {
            return Objects.hash(i, s);
        }

        @Override
        public String toString() {
            return "SValue{" + "i=" + i + ", s='" + s + '\'' + '}';
        }

    }

    @Test
    public void testParameterAccessor() {
        TransientStore ts = tss.getStore("testStore");

        String key = "key1";
        assertFalse(ts.exists(key));
        // setCompleted is not creating an entry
        ts.setCompleted(key, true);
        assertFalse(ts.exists(key));
        assertFalse(ts.isCompleted(key));

        ts.putParameter(key, "foo", "bar");
        assertTrue(ts.exists(key));
        assertFalse(ts.isCompleted(key));
        ts.putParameter(key, "foo2", "baz");
        assertEquals("bar", ts.getParameter(key, "foo"));
        assertEquals("baz", ts.getParameter(key, "foo2"));
        ts.putParameter(key, "foo2", "baz2");
        assertEquals("baz2", ts.getParameter(key, "foo2"));
        ts.putParameter(key, "foo2", null);
        assertNull(ts.getParameter(key, "foo2"));
        assertEquals("bar", ts.getParameter(key, "foo"));
        ts.setCompleted(key, true);
        assertTrue(ts.isCompleted(key));

        ts.remove(key);
        assertFalse(ts.exists(key));
        assertFalse(ts.isCompleted(key));
        // no effect
        ts.release(key);
        ts.remove(key);

        Map<String, Serializable> params = new HashMap<>();
        params.put("foo", "bar");
        params.put("foo2", "baz");
        ts.putParameters(key, params);
        assertTrue(ts.exists(key));
        assertFalse(ts.isCompleted(key));
        assertEquals("bar", ts.getParameter(key, "foo"));
        assertEquals("baz", ts.getParameter(key, "foo2"));
        assertNull(ts.getParameter(key, "unknown_param"));
        assertNull(ts.getParameter("unknown_key", "unknown_param"));

        params.clear();
        params.put("p1", "123");
        ts.putParameters(key, params);
        assertNull(ts.getParameter(key, "foo"));
        assertNull(ts.getParameter(key, "foo2"));
        assertEquals("123", ts.getParameter(key, "p1"));

        assertEquals(params, ts.getParameters(key));

        ts.release(key);
        ts.remove(key);

        ts.putParameter(key, "foo", null);
        ts.putParameter(key, "bar", null);
        assertTrue(ts.exists(key));
        assertNull(ts.getParameter(key, "foo"));
        assertNull(ts.getParameter(key, "bar"));
        assertNull(ts.getParameter(key, "unknown"));
        ts.remove(key);
    }

    @Test
    public void testPuParameterWithSerializableValue() {
        TransientStore ts = tss.getStore("testStore");
        String key = "key2";
        assertFalse(ts.exists(key));

        SValue value = new SValue(42, "foo");
        ts.putParameter(key, "foo", "baz");
        ts.putParameter(key, "bar", 123);
        ts.putParameter(key, "binary", value);
        assertEquals("baz", ts.getParameter(key, "foo"));
        assertEquals(123, ts.getParameter(key, "bar"));
        assertEquals(value, ts.getParameter(key, "binary"));

        Map<String, Serializable> params = ts.getParameters(key);
        assertEquals("baz", params.get("foo"));
        assertEquals(123, params.get("bar"));
        assertEquals(value, params.get("binary"));
        ts.remove(key);

        ts.putParameters(key, params);
        assertEquals("baz", params.get("foo"));
        assertEquals(123, params.get("bar"));
        assertEquals(value, params.get("binary"));

        assertNull(ts.getParameter(key, "unknown"));
        ts.remove(key);

        assertNull(ts.getParameter(key, "unknown"));
        assertNull(ts.getParameters(key));
    }

    @Test
    public void testBlobAccessor() {
        TransientStore ts = tss.getStore("testStore");
        TransientStoreProvider tsm = (TransientStoreProvider) ts;

        String key = "key1";
        assertFalse(ts.exists(key));
        assertNull(ts.getBlobs(key));
        assertEquals(-1, ts.getSize(key));
        assertEquals(0, tsm.getStorageSize());

        ts.putParameter(key, "foo", "bar");
        assertEquals(0, tsm.getStorageSize());
        assertNotNull(ts.getBlobs(key));
        assertTrue(ts.getBlobs(key).isEmpty());
        ts.remove(key);

        ts.putBlobs(key, Collections.singletonList(new StringBlob("blah")));
        assertTrue(ts.exists(key));
        List<Blob> blobs = ts.getBlobs(key);
        assertEquals(1, blobs.size());
        assertEquals("text/plain", blobs.get(0).getMimeType());
        assertEquals(4, ts.getSize(key));
        assertEquals(4, tsm.getStorageSize());
        // key exists without params
        assertNull(ts.getParameter(key, "unknown"));
        assertNotNull(ts.getParameters(key));
        assertTrue(ts.getParameters(key).isEmpty());

        ts.putBlobs("bar", Collections.singletonList(new StringBlob("blah 2")));
        assertEquals(6, tsm.getSize("bar"));
        assertEquals(10, tsm.getStorageSize());
        ts.remove(key);
        assertEquals(6, tsm.getStorageSize());
        ts.remove("bar");
        assertEquals(0, tsm.getStorageSize());
    }
}
