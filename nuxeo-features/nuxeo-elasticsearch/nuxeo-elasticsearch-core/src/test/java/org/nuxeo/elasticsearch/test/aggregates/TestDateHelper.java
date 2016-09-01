/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Benoit Delbosc
 */

package org.nuxeo.elasticsearch.test.aggregates;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.nuxeo.elasticsearch.aggregate.DateHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TestDateHelper {

    @Test
    public void testPlusDuration() {
        DateTime now = new DateTime();
        DateTime ret = DateHelper.plusDuration(now, "3w");
        assertEquals(now.plusWeeks(3), ret);

        ret = DateHelper.plusDuration(now, "week");
        assertEquals(now.plusWeeks(1), ret);

        ret = DateHelper.plusDuration(now, "2y");
        assertEquals(now.plusYears(2), ret);

        ret = DateHelper.plusDuration(now, "8h");
        assertEquals(now.plusHours(8), ret);

        ret = DateHelper.plusDuration(now, "quarter");
        assertEquals(now.plusMonths(3), ret);

        ret = DateHelper.plusDuration(now, "1234");
        assertEquals(now.plusMillis(1234), ret);

        ret = DateHelper.plusDuration(now, "7");
        assertEquals(now.plusMillis(7), ret);
    }

    @Test
    public void testPlusDurationInvalid() {
        DateTime now = new DateTime();
        DateTime ret = null;

        try {
            ret = DateHelper.plusDuration(now, "days");
            Assert.fail("Exception not raise");
        } catch (IllegalArgumentException e) {
            assertNull(ret);
        }
        try {
            ret = DateHelper.plusDuration(now, "3 d");
            Assert.fail("Exception not raise");
        } catch (IllegalArgumentException e) {
            assertNull(ret);
        }
        try {
            ret = DateHelper.plusDuration(now, "3.2d");
            Assert.fail("Exception not raise");
        } catch (IllegalArgumentException e) {
            assertNull(ret);
        }
        try {
            ret = DateHelper.plusDuration(now, "foos");
            Assert.fail("Exception not raise");
        } catch (IllegalArgumentException e) {
            assertNull(ret);
        }
        try {
            ret = DateHelper.plusDuration(now, "");
            Assert.fail("Exception not raise");
        } catch (IllegalArgumentException e) {
            assertNull(ret);
        }
    }
}
