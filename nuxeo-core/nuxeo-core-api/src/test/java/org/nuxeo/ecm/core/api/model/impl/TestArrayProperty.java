/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.api.model.impl;

import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;

import org.junit.Test;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;

public class TestArrayProperty extends AbstractTestProperty {

    /*
     * NXP-20335
     */
    @Test
    public void testArrayOfIntOnLongProperty() {
        ArrayProperty property = getArrayProperty(LongType.INSTANCE);
        property.setValue(new Integer[] { 1, 2, 3 });
        assertArrayEquals(new Long[] { 1L, 2L, 3L }, (Long[]) property.getValue());
    }

    /*
     * NXP-20335
     */
    @Test
    public void testCollectionOfIntOnLongProperty() {
        ArrayProperty property = getArrayProperty(LongType.INSTANCE);
        property.setValue(Arrays.asList(1, 2, 3));
        assertArrayEquals(new Long[] { 1L, 2L, 3L }, (Long[]) property.getValue());
    }

}
