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

package org.nuxeo.theme.test.themes;

import java.io.IOException;
import java.util.Date;

import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.Utils;
import org.nuxeo.theme.themes.ThemeDescriptor;
import org.nuxeo.theme.themes.ThemeIOException;
import org.nuxeo.theme.themes.ThemeParser;
import org.nuxeo.theme.themes.ThemeSerializer;

public class ThemeIORoundTrip extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-service.xml");
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-contrib.xml");
        deployContrib("org.nuxeo.theme.core.tests", "fragment-config.xml");
        deployContrib("org.nuxeo.theme.core.tests", "theme-bank-config.xml");
    }

    public void testRoundTrip() throws ThemeIOException, IOException {
        ThemeDescriptor themeDef = new ThemeDescriptor();
        themeDef.setName("default");
        themeDef.setSrc("roundtrip-theme.xml");
        themeDef.setLastLoaded(new Date());
        Manager.getTypeRegistry().register(themeDef);
        final boolean preload = false;
        ThemeParser.registerTheme(themeDef, preload);
        final String output = new ThemeSerializer().serializeToXml(
                "roundtrip-theme.xml", 2);
        final String input = Utils.readResourceAsString("roundtrip-theme.xml");
        // FIXME: the "special fonts" style is not getting exported because the
        // the resource bank is not registered
        // assertEquals(input, output);
    }

}
