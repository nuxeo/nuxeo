/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.test.events;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.events.EventContext;
import org.nuxeo.theme.events.EventManager;
import org.nuxeo.theme.events.EventType;
import org.nuxeo.theme.services.ThemeService;
import org.nuxeo.theme.test.DummyEventListener;
import org.nuxeo.theme.types.TypeRegistry;

public class TestEvents extends NXRuntimeTestCase {

    private EventManager eventManager;

    private TypeRegistry typeRegistry;

    private EventType changed;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.theme.core.tests", "nxthemes-core-service.xml");
        deployContrib("org.nuxeo.theme.core.tests", "nxthemes-core-contrib.xml");

        ThemeService themeService = (ThemeService) Framework.getRuntime().getComponent(
                ThemeService.ID);
        eventManager = (EventManager) themeService.getRegistry("events");
        typeRegistry = (TypeRegistry) themeService.getRegistry("types");

        changed = new EventType("changed");
        typeRegistry.register(changed);
    }

    @Override
    public void tearDown() throws Exception {
        typeRegistry.unregister(changed);
        typeRegistry = null;
        eventManager.clear();
        eventManager = null;
        changed = null;
        super.tearDown();
    }

    public void testEvents() {
        DummyEventListener changedEventListener = new DummyEventListener(
                changed);

        assertSame(changed, changedEventListener.getEventType());
        assertTrue(eventManager.getListenersFor(changed).isEmpty());

        eventManager.addListener(changedEventListener);
        assertTrue(eventManager.getListenersFor(changed).contains(
                changedEventListener));

        assertEquals(0, changedEventListener.counter);
        eventManager.notify("changed", new EventContext(null, null));
        assertEquals(1, changedEventListener.counter);

        assertSame(changed, eventManager.getEventType("changed"));

        eventManager.removeListener(changedEventListener);
        assertTrue(eventManager.getListenersFor(changed).isEmpty());
    }

}
