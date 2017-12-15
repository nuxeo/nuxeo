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
import static org.junit.Assert.assertNotEquals;

import java.text.ParseException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.Test;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.core.schema.utils.DateParser;

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

    @Test
    public void testArrayOfStrings() {
        ArrayProperty property = getArrayProperty(StringType.INSTANCE);
        String[] testStrings = new String[] {"there", null, "once", "was a dog"};
        property.setValue(testStrings);
        assertArrayEquals(testStrings, (String[]) property.getValue());
    }

    @Test
    public void testArrayOfDoubles() {
        ArrayProperty property = getArrayProperty(DoubleType.INSTANCE);

        property.setValue(new String[] { "1", "2", "3" });
        assertArrayEquals(new Double[] { 1d, 2d, 3d }, (Double[]) property.getValue());

        property.setValue(new Object[] { "1", 2, "3", 9, null });
        assertArrayEquals(new Double[] { 1d, 2d, 3d, 9d, null }, (Double[]) property.getValue());

        property.setValue(Arrays.asList(1f, 5f, 123.4f));
        assertArrayEquals(new Double[] { 1d, 5d, (double)123.4f }, (Double[]) property.getValue());
    }

    @Test
    public void testArrayOfIntegers() {
        ArrayProperty property = getArrayProperty(IntegerType.INSTANCE);

        property.setValue(new String[] {"1","2", null,"3"});
        assertArrayEquals(new Long[] { 1L, 2L, null, 3L }, (Long[]) property.getValue());

        property.setValue(Arrays.asList(1, "2", 3));
        assertArrayEquals(new Long[] { 1L, 2L, 3L }, (Long[]) property.getValue());
    }

    @Test
    public void testArrayOfDates() {
        ArrayProperty property = getArrayProperty(DateType.INSTANCE);

        String myTestDate = "2017-11-29T12:30-03:44";
        // Parse with timezone
        Date date = DateParser.parseW3CDateTime(myTestDate);
        // Now use the system default timezone
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        property.setValue(new Object[] { null, cal, date, myTestDate });
        assertArrayEquals(new Object[] { null, cal, cal, cal }, (Calendar[]) property.getValue());

        // The DateProperty always stores using the system default timezone, unless you pass your own Calendar
        Calendar calWithCorrectTimezone = GregorianCalendar.from(ZonedDateTime.parse(myTestDate));
        assertNotEquals(cal, calWithCorrectTimezone);
        property.setValue(new Object[] { null, calWithCorrectTimezone });
        assertArrayEquals(new Object[] { null, calWithCorrectTimezone }, (Calendar[]) property.getValue());
    }

    @Test
    public void testArrayOfBooleanProperty() {
        ArrayProperty property = getArrayProperty(BooleanType.INSTANCE);

        property.setValue(new Object[] { null, "true", false, 0, 1 });
        assertArrayEquals(new Boolean[] { null, true, false, false, true }, (Boolean[]) property.getValue());
    }
}
