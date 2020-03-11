/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     matic
 */
package org.nuxeo.runtime.management.counters;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.javasimon.Counter;
import org.javasimon.jmx.CounterSample;

/**
 * @author matic
 */
public class CounterMXBeanImpl extends org.javasimon.jmx.CounterMXBeanImpl implements CounterMXBean {

    public CounterMXBeanImpl(Counter counter) {
        super(counter);
    }

    @Override
    public String sampleAsString() {
        CounterSample sample = sample();
        return sample.toString();
    }

    protected void doFillMap(CounterSample sample, Map<String, Serializable> map, Class<?> clazz) {
        if (clazz == null) {
            return;
        }
        if (Object.class.equals(clazz)) {
            return;
        }
        for (Field f : clazz.getDeclaredFields()) {
            try {
                f.setAccessible(true);
                map.put(f.getName(), (Serializable) f.get(sample));
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        doFillMap(sample, map, clazz.getSuperclass());
    }

    @Override
    public Map<String, Serializable> sampleAsMap() {
        HashMap<String, Serializable> map = new HashMap<>();
        CounterSample sample = sample();
        doFillMap(sample, map, sample.getClass());
        return map;
    }
}
