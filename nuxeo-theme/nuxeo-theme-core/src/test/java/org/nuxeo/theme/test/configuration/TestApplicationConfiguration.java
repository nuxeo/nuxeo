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

package org.nuxeo.theme.test.configuration;

import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.ApplicationType;
import org.nuxeo.theme.CachingDef;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.NegotiationDef;
import org.nuxeo.theme.ViewDef;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;

public class TestApplicationConfiguration extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-service.xml");
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-contrib.xml");
        deployContrib("org.nuxeo.theme.core.tests", "application-config.xml");
    }

    public void testRegisterApplication1() {
        TypeRegistry typeRegistry = Manager.getTypeRegistry();
        ApplicationType app1a = (ApplicationType) typeRegistry.lookup(
                TypeFamily.APPLICATION, "/my-app");
        assertEquals("/my-app", app1a.getTypeName());
        assertEquals("html", app1a.getTemplateEngine());

        NegotiationDef negotiation = app1a.getNegotiation();
        assertEquals("default-strategy", negotiation.getStrategy());
        assertEquals("default-theme", negotiation.getDefaultTheme());
        assertEquals("default-perspective", negotiation.getDefaultPerspective());
        assertEquals("default-engine", negotiation.getDefaultEngine());

        CachingDef resourceCaching = app1a.getResourceCaching();
        assertEquals("72000", resourceCaching.getLifetime());

        CachingDef styleCaching = app1a.getStyleCaching();
        assertEquals("600", styleCaching.getLifetime());

        assertTrue(app1a.getViewIds().contains("/workspaces.xhtml"));
        assertTrue(app1a.getViewIds().contains("/login.xhtml"));
        assertTrue(app1a.getViewIds().contains("/printable.xhtml"));
        ViewDef view1 = app1a.getViewById("/workspaces.xhtml");
        assertEquals("default/workspaces", view1.getTheme());
        ViewDef view2 = app1a.getViewById("/login.xhtml");
        assertEquals("login", view2.getPerspective());
        ViewDef view3 = app1a.getViewById("/printable.xhtml");
        assertEquals("printable", view3.getEngine());
    }

    public void testRegisterApplication2() {
        TypeRegistry typeRegistry = Manager.getTypeRegistry();
        ApplicationType app2a = (ApplicationType) typeRegistry.lookup(
                TypeFamily.APPLICATION, "/my-app2");
        assertEquals("freemarker", app2a.getTemplateEngine());
        assertNull(app2a.getNegotiation());
        assertNull(app2a.getResourceCaching());
        assertNull(app2a.getStyleCaching());
        assertTrue(app2a.getViewDefs().isEmpty());
    }

    public void testOverrideProperties() throws Exception {
        TypeRegistry typeRegistry = Manager.getTypeRegistry();
        // Override default application settings
        deployContrib("org.nuxeo.theme.core.tests",
                "application-config-override.xml");
        ApplicationType app1b = (ApplicationType) typeRegistry.lookup(
                TypeFamily.APPLICATION, "/my-app");

        // Existing properties are left unchanged
        assertTrue(app1b.getViewIds().contains("/workspaces.xhtml"));
        assertTrue(app1b.getViewIds().contains("/login.xhtml"));
        assertTrue(app1b.getViewIds().contains("/printable.xhtml"));
        ViewDef view1 = app1b.getViewById("/workspaces.xhtml");
        assertEquals("default/workspaces", view1.getTheme());
        ViewDef view2 = app1b.getViewById("/login.xhtml");
        assertEquals("login", view2.getPerspective());
        ViewDef view3 = app1b.getViewById("/printable.xhtml");
        assertEquals("printable", view3.getEngine());

        // Overridden properties
        assertEquals("jsf-facelets", app1b.getTemplateEngine());
        NegotiationDef negotiation = app1b.getNegotiation();
        assertEquals("my-strategy", negotiation.getStrategy());
        assertEquals("my-theme", negotiation.getDefaultTheme());
        assertEquals("my-perspective", negotiation.getDefaultPerspective());
        assertEquals("my-engine", negotiation.getDefaultEngine());

        CachingDef resourceCaching = app1b.getResourceCaching();
        assertEquals("100", resourceCaching.getLifetime());

        CachingDef styleCaching = app1b.getStyleCaching();
        assertEquals("60", styleCaching.getLifetime());

        ViewDef view4 = app1b.getViewById("/my-view.xhtml");
        assertEquals("my-theme/default", view4.getTheme());
    }

    public void testOverrideProperties2() throws Exception {
        TypeRegistry typeRegistry = Manager.getTypeRegistry();
        // Override default application settings
        deployContrib("org.nuxeo.theme.core.tests",
                "application-config-override.xml");
        ApplicationType app2b = (ApplicationType) typeRegistry.lookup(
                TypeFamily.APPLICATION, "/my-app2");

        // Existing properties are left unchanged
        assertEquals("freemarker", app2b.getTemplateEngine());

        // Overridden properties
        NegotiationDef negotiation = app2b.getNegotiation();
        assertEquals("my-strategy", negotiation.getStrategy());
        assertEquals("my-theme", negotiation.getDefaultTheme());
        assertEquals("my-perspective", negotiation.getDefaultPerspective());
        assertEquals("my-engine", negotiation.getDefaultEngine());

        CachingDef resourceCaching = app2b.getResourceCaching();
        assertEquals("100", resourceCaching.getLifetime());

        CachingDef styleCaching = app2b.getStyleCaching();
        assertEquals("60", styleCaching.getLifetime());

        ViewDef view4 = app2b.getViewById("/my-view.xhtml");
        assertEquals("my-theme/default", view4.getTheme());
    }

}
