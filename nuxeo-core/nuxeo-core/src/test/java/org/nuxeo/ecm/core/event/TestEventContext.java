/*
 * (C) Copyright 2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 *     Bogdan Stefanescu
 */

package org.nuxeo.ecm.core.event;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.local.LocalSession;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * This test probably should be in
 * org.nuxeo.ecm.core.event.test.EventListenerTest but this would create
 * dependency loops.
 *
 * @author Bogdan Stefanescu
 * @author Florent Guillaume
 */
public class TestEventContext extends NXRuntimeTestCase {

    public void testEventContext() {
        EventContextImpl ctx = new EventContextImpl("arg1", "arg2");
        CoreSession cs = new LocalSession();
        ctx.setCoreSession(cs);
        assertEquals(cs, ctx.getCoreSession());
        assertEquals("arg1", ctx.getArguments()[0]);
        assertEquals("arg2", ctx.getArguments()[1]);
        ctx.setProperty("p1", "v1");
        assertEquals("v1", ctx.getProperty("p1"));
        assertEquals(1, ctx.getProperties().size());
        ctx.getProperties().clear();
        assertEquals(0, ctx.getProperties().size());
        assertNull(ctx.getPrincipal());
    }

}
