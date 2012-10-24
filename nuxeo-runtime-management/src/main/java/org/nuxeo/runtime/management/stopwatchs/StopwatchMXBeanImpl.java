/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     matic
 */
package org.nuxeo.runtime.management.stopwatchs;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.javasimon.Stopwatch;
import org.javasimon.StopwatchSample;

/**
 * @author matic
 *
 */
public class StopwatchMXBeanImpl extends org.javasimon.jmx.StopwatchMXBeanImpl implements StopwatchMXBean {

	public StopwatchMXBeanImpl(Stopwatch stopwatch) {
		super(stopwatch);
	}

    @Override
    public String sampleAsString() {
        return sample().toString();
    }

        protected void doFillMap(StopwatchSample sample, Map<String,Serializable> map, Class<?> clazz) {
        if (clazz == null) {
            return;
        }
        if (Object.class.equals(clazz)) {
            return;
        }
        for (Field f:clazz.getDeclaredFields()) {
            try {
                f.setAccessible(true);
                map.put(f.getName(), (Serializable)f.get(sample));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        doFillMap(sample, map, clazz.getSuperclass());
    }
        
    @Override
    public Map<String, Serializable> sampleAsMap() {
        Map<String,Serializable> map = new HashMap<String,Serializable>();
        StopwatchSample sample = sample();
        doFillMap(sample, map, sample.getClass());
        return map;
    }

	
}
