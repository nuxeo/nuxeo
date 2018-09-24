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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.avro.reflect.AvroEncode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.core.bulk.message.MapAsJsonAsStringEncoding;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features(CoreBulkFeature.class)
public class TestMapAsJsonAsStringEncoding {

    @Rule
    public final CodecTestRule<BeanWithMap> codecRule = new CodecTestRule<>("avro", BeanWithMap.class);

    @Test
    public void testEmptyMap() {
        BeanWithMap bean = new BeanWithMap();
        BeanWithMap actualBean = codecRule.encodeDecode(bean);

        assertNotNull(actualBean.getMap());
        assertTrue(actualBean.getMap().isEmpty());
    }

    @Test
    public void testSimpleMap() {
        BeanWithMap bean = new BeanWithMap();
        bean.getMap().put("key1", "value");
        bean.getMap().put("key2", 20);
        bean.getMap().put("key3", true);
        BeanWithMap actualBean = codecRule.encodeDecode(bean);

        assertNotNull(actualBean.getMap());
        assertFalse(actualBean.getMap().isEmpty());
        assertEquals(bean.getMap(), actualBean.getMap());
    }

    @Test
    public void testComplexMap() {
        HashMap<String, Serializable> subMap = new HashMap<>();
        subMap.put("subKey1", "value");
        subMap.put("subKey2", 20);
        subMap.put("subKey3", true);
        BeanWithMap bean = new BeanWithMap();
        bean.getMap().put("key1", subMap);
        BeanWithMap actualBean = codecRule.encodeDecode(bean);

        assertNotNull(actualBean.getMap());
        assertFalse(actualBean.getMap().isEmpty());
        assertEquals(bean.getMap(), actualBean.getMap());
    }

    public static class BeanWithMap {

        @AvroEncode(using = MapAsJsonAsStringEncoding.class)
        protected final Map<String, Serializable> map = new HashMap<>();

        public Map<String, Serializable> getMap() {
            return map;
        }
    }

}
