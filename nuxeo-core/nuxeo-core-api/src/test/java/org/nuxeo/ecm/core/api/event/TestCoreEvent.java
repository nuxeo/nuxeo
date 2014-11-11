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

package org.nuxeo.ecm.core.api.event;

import junit.framework.TestCase;

import org.nuxeo.ecm.core.api.event.impl.CoreEventImpl;


public class TestCoreEvent extends TestCase {

    public void testDefaultConstructor() {
        CoreEvent ev = new CoreEventImpl(null, null, null, null, null, null);

        assertNull(ev.getCategory());
        assertNull(ev.getComment());
        assertNull(ev.getEventId());
        assertEquals(2, ev.getInfo().size());
        assertNull(ev.getPrincipal());
        assertNull(ev.getSource());
        assertNotNull(ev.getDate());
    }

}
