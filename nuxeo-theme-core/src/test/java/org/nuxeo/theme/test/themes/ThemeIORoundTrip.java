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
import java.net.URL;

import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.Utils;
import org.nuxeo.theme.elements.ThemeElement;
import org.nuxeo.theme.themes.ThemeParser;
import org.nuxeo.theme.themes.ThemeSerializer;

public class ThemeIORoundTrip extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.theme.core", "OSGI-INF/nxthemes-core-service.xml");
        deployContrib("org.nuxeo.theme.core", "OSGI-INF/nxthemes-core-contrib.xml");
        deployContrib("fragment-config.xml");
    }

    @Override
    public void tearDown() throws Exception {
        Manager.getRelationStorage().clear();
        Manager.getPerspectiveManager().clear();
        Manager.getTypeRegistry().clear();
        Manager.getUidManager().clear();
        super.tearDown();
    }

    public void testRoundTrip() throws IOException {
        final URL url = getClass().getClassLoader().getResource("roundtrip-theme.xml");
        ThemeParser.registerTheme(url);
        final ThemeElement theme = Manager.getThemeManager().getThemeByName("default");
        final String output = new ThemeSerializer().serializeToXml(theme, 2).replace("/>", " />");
        final String input = Utils.getFileContent("roundtrip-theme.xml");
        assertEquals(input, output);
    }

}
