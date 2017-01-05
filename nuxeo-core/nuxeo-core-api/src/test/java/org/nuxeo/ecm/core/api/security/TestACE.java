/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.api.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestACE {

    private ACE ace;

    private ACE acebis;

    @Before
    public void setUp() {
        ace = new ACE("bogdan", "write", false);
        acebis = ACE.builder("vlad", "write")
                    .creator("pas")
                    .begin(new GregorianCalendar(2015, Calendar.JULY, 14, 12, 34, 56))
                    .end(new GregorianCalendar(2015, Calendar.AUGUST, 14, 12, 34, 56))
                    .build();
    }

    @After
    public void tearDown() {
        ace = null;
    }

    @Test
    public void testGetType() {
        assertFalse(ace.isGranted());
        assertTrue(ace.isDenied());
    }

    @Test
    public void testGetPrincipals() {
        assertEquals("bogdan", ace.getUsername());
    }

    @Test
    public void testGetPermissions() {
        assertEquals("write", ace.getPermission());
    }

    @SuppressWarnings({ "ObjectEqualsNull" })
    @Test
    public void testEquals() {
        ACE ace2 = new ACE("bogdan", "write", false);
        ACE ace3 = new ACE("raoul", "write", false);
        ACE ace4 = new ACE("bogdan", "read", false);
        ACE ace5 = new ACE("bogdan", "write", true);

        assertEquals(ace, ace);
        assertEquals(ace, ace2);
        assertEquals(ace2, ace);
        assertFalse(ace.equals(null));
        assertFalse(ace.equals(ace3));
        assertFalse(ace.equals(ace4));
        assertFalse(ace.equals(ace5));

        assertEquals(ace.hashCode(), ace2.hashCode());

        Calendar begin = new GregorianCalendar(2015, Calendar.JULY, 14, 12, 34, 56);
        Calendar end = new GregorianCalendar(2015, Calendar.AUGUST, 14, 12, 34, 56);
        ACE ace6 = ACE.builder("leela", "read").isGranted(false).begin(begin).end(end).build();
        ACE ace7 = ACE.builder("leela", "read").isGranted(false).begin(begin).end(end).build();
        assertEquals(ace6, ace7);

        ACE ace8 = ACE.fromId(ace7.getId());
        assertEquals(ace6, ace8);
    }

    @Test
    public void testNewConstructors() {
        Calendar cal1 = new GregorianCalendar(2015, Calendar.JULY, 14, 12, 34, 56);
        Calendar cal2 = new GregorianCalendar(2015, Calendar.AUGUST, 14, 12, 34, 56);
        ACE ace1 = ACE.builder("vlad", "write").creator("pas").begin(cal1).end(cal2).build();
        assertEquals(acebis, ace1);
    }

    @Test
    public void testACEId() {
        Date now = new Date();
        Calendar cal1 = new GregorianCalendar();
        cal1.setTimeInMillis(now.toInstant().minus(5, ChronoUnit.DAYS).toEpochMilli());
        Calendar cal2 = new GregorianCalendar();
        cal2.setTimeInMillis(now.toInstant().plus(5, ChronoUnit.DAYS).toEpochMilli());
        ACE ace = ACE.builder("vlad", "write").creator("pas").begin(cal1).end(cal2).build();

        assertEquals("vlad:write:true:pas:" + cal1.getTimeInMillis() + ":" + cal2.getTimeInMillis(),
                ace.getId());

        String aceId = "bob:write:false:pablo:" + cal1.getTimeInMillis() + ":" + cal2.getTimeInMillis();
        ace = ACE.fromId(aceId);
        assertNotNull(ace);
        assertEquals("bob", ace.getUsername());
        assertEquals("write", ace.getPermission());
        assertFalse(ace.isGranted());
        assertEquals("pablo", ace.getCreator());
        assertEquals(cal1, ace.getBegin());
        assertEquals(cal2, ace.getEnd());
        assertTrue(ace.isEffective());

        aceId = "pedro:read:true:::";
        ace = ACE.fromId(aceId);
        assertNotNull(ace);
        assertEquals("pedro", ace.getUsername());
        assertEquals("read", ace.getPermission());
        assertTrue(ace.isGranted());
        assertNull(ace.getCreator());
        assertNull(ace.getBegin());
        assertNull(ace.getEnd());
        assertTrue(ace.isEffective());

        aceId = "pedro:read:true::" + cal1.getTimeInMillis() + ":";
        ace = ACE.fromId(aceId);
        assertNotNull(ace);
        assertEquals("pedro", ace.getUsername());
        assertEquals("read", ace.getPermission());
        assertTrue(ace.isGranted());
        assertNull(ace.getCreator());
        assertEquals(cal1, ace.getBegin());
        assertNull(ace.getEnd());
        assertTrue(ace.isEffective());

        // Tests with a username that includes colons ":"
        aceId = "mycorp:dep:research:project23:read:true::" + cal1.getTimeInMillis() + ":";
        ace = ACE.fromId(aceId);
        assertNotNull(ace);
        assertEquals("mycorp:dep:research:project23", ace.getUsername());
        assertEquals("read", ace.getPermission());
        assertTrue(ace.isGranted());
        assertEquals(cal1, ace.getBegin());
        assertNull(ace.getEnd());
        assertTrue(ace.isEffective());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidACEId() {
        String aceId = "pedro:read:";
        ACE.fromId(aceId);
    }
}
