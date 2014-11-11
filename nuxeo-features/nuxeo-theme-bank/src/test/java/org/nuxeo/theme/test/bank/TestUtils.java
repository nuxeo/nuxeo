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

package org.nuxeo.theme.test.bank;

import java.io.IOException;
import java.util.List;

import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.bank.Utils;
import org.nuxeo.theme.themes.ThemeManager;
import org.nuxeo.theme.types.TypeRegistry;

public class TestUtils extends NXRuntimeTestCase {

    private ThemeManager themeManager;

    private TypeRegistry typeRegistry;

    private final String BANK_NAME = "test";

    private final String COLLECTION_NAME = "Test";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-service.xml");
        deployContrib("org.nuxeo.theme.core",
                "OSGI-INF/nxthemes-core-contrib.xml");
        deployContrib("org.nuxeo.theme.bank.tests", "theme-bank-config.xml");
        themeManager = Manager.getThemeManager();
        typeRegistry = Manager.getTypeRegistry();
    }

    @Override
    public void tearDown() throws Exception {
        Manager.getTypeRegistry().clear();
        themeManager.clear();
        themeManager = null;
        typeRegistry.clear();
        typeRegistry = null;
        super.tearDown();
    }

    public void testListSkinsInCollection() {
        final List<String> skins = Utils.listSkinsInCollection(BANK_NAME,
                COLLECTION_NAME);
        assertEquals("test.css", skins.get(0));
        assertEquals("skin-without-preview.css", skins.get(1));
    }

    // JSON
    public void testListBankSkins() throws IOException {
        final String expected = org.nuxeo.theme.Utils.readResourceAsString("skins.json");
        assertEquals(expected, Utils.listBankSkins(BANK_NAME));
    }

    public void testListImages() throws IOException {
        assertEquals("[\"Test/emoticon_smile.png\",\"Test/photo.png\"]",
                Utils.listImages(BANK_NAME));
    }

    public void disabledTestGetNavTree() throws IOException {
        final String expected = org.nuxeo.theme.Utils.readResourceAsString("navtree.json");
        assertEquals(expected, Utils.getNavTree());
    }

}
