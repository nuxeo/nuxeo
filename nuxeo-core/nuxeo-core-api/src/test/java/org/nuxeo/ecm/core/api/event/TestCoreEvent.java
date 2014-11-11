/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.api.event;

import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.event.impl.CoreEventImpl;


public class TestCoreEvent {

    @Test
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
