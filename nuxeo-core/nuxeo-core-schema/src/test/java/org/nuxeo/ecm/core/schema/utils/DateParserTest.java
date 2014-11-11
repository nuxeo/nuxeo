/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.ecm.core.schema.utils;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.Date;

import org.junit.Test;

/**
 * @author dmetzler
 *
 */
public class DateParserTest {

    @Test
    public void parseTestDate() throws Exception {
        //SUPNXP-7186 that threw an exception
        Calendar cal = DateParser.parse("2012-11-29T13:15:18.176+0000");
        assertEquals(13,cal.get(Calendar.HOUR_OF_DAY));

        cal = DateParser.parse("2012-11-29T13:15:18.176+0200");
        assertEquals(11,cal.get(Calendar.HOUR_OF_DAY));
    }


    @Test
    public void parseTimeZone() throws Exception {
        Calendar cal = DateParser.parse("2012-11-29T13:15:18.176Z");
        assertEquals(13,cal.get(Calendar.HOUR_OF_DAY));

        cal = DateParser.parse("2012-11-29T13:15:18.176+0100");
        assertEquals(12,cal.get(Calendar.HOUR_OF_DAY));

        cal = DateParser.parse("2012-11-29T13:15:18.176+01:00");
        assertEquals(12,cal.get(Calendar.HOUR_OF_DAY));

        cal = DateParser.parse("2012-11-29T13:15:18.176+01");
        assertEquals(12,cal.get(Calendar.HOUR_OF_DAY));


        cal = DateParser.parse("2012-11-29T13:15:18.176-0100");
        assertEquals(14,cal.get(Calendar.HOUR_OF_DAY));


    }
}
