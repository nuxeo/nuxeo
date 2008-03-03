/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.model;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.nuxeo.ecm.core.utils.SIDGenerator;

public class TestSidGenerator extends TestCase {

    public void testGenerator() throws Exception {
        System.out.println("This test will take several seconds ... ");
        // generate 1 000 000 (one million ids)
        Set<Long> ids = new HashSet<Long>();
        for (int i = 0; i < 1000000; i++) {
            long id = SIDGenerator.next();
//            System.out.println("ID: " + id + " : "
//                    + Long.toHexString(id).toUpperCase());
            if (!ids.add(id)) {
                fail("ID already generated");
            }
        }
    }

    public void testGeneratorReset() throws Exception {
        // generate 100 000 ids
        Set<Long> ids = new HashSet<Long>();
        for (int i = 0; i < 100000; i++) {
            long id = SIDGenerator.next();
//            System.out.println("ID: " + id + " : "
//                    + Long.toHexString(id).toUpperCase());
            if (!ids.add(id)) {
                fail("ID already generated");
            }
        }

        // change the counter to a value near the max one to force a counter reset
        Field field = SIDGenerator.class.getDeclaredField("count");
        field.setAccessible(true);
        field.set(null, Integer.MAX_VALUE - 50000);

        // generate another 1 000 000 (one million of ids)
        for (int i = 0; i < 100000; i++) {
            long id = SIDGenerator.next();
            if (!ids.add(id)) {
                fail("ID already generated");
            }
        }

        Integer counter = (Integer) field.get(null);
        assertEquals(50000, counter.intValue());
    }

}
