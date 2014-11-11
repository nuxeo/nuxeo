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

    public void testGetCollections() throws IOException {
        final List<String> collections = Utils.getCollections(BANK_NAME);
        assertEquals("Test", collections.get(0));
    }

    public void testGetStyleItemsInCollections() throws IOException {
        final List<String> items = Utils.getItemsInCollection(BANK_NAME,
                COLLECTION_NAME, "style");
        assertEquals(5, items.size());
        assertTrue(items.contains("base-skin.css"));
        assertTrue(items.contains("style.css"));
        assertTrue(items.contains("main.css"));
        assertTrue(items.contains("test.css"));
        assertTrue(items.contains("style-with-preview.css"));
    }

    public void testGetPresetItemsInCollections() throws IOException {
        final List<String> items = Utils.getItemsInCollection(BANK_NAME,
                COLLECTION_NAME, "preset");
        assertEquals(4, items.size());
        assertTrue(items.contains("font"));
        assertTrue(items.contains("color"));
        assertTrue(items.contains("background"));
        assertTrue(items.contains("border"));
    }

    public void testGetImageItemsInCollections() throws IOException {
        final List<String> items = Utils.getItemsInCollection(BANK_NAME,
                COLLECTION_NAME, "image");
        assertEquals(2, items.size());
        assertTrue(items.contains("emoticon_smile.png"));
        assertTrue(items.contains("photo.png"));
    }

    public void testListSkinsInCollection() throws IOException {
        final List<String> skins = Utils.listSkinsInCollection(BANK_NAME,
                COLLECTION_NAME);
        assertEquals(3, skins.size());
        assertTrue(skins.contains("test.css"));
        assertTrue(skins.contains("skin-without-preview.css"));
        assertTrue(skins.contains("base-skin.css"));
    }

    // JSON
    public void testListCollections() throws IOException {
        assertEquals("[\"Test\"]", Utils.listCollections(BANK_NAME));
    }

    public void testListBankSkins() throws IOException {
        final String expected = org.nuxeo.theme.Utils.readResourceAsString("skins.json");
        assertEquals(expected, Utils.listBankSkins(BANK_NAME));
    }

    public void testListImages() throws IOException {
        assertEquals(
                "[{\"name\":\"emoticon_smile.png\",\"collection\":\"Test\"},{\"name\":\"photo.png\",\"collection\":\"Test\"}]",
                Utils.listImages(BANK_NAME));
    }

    public void disabledTestListStyles() throws IOException {
        assertEquals(org.nuxeo.theme.Utils.readResourceAsString("styles.json"),
                Utils.listBankStyles(BANK_NAME));
    }

    public void disabledTestGetNavTree() throws IOException {
        final String expected = org.nuxeo.theme.Utils.readResourceAsString("navtree.json");
        assertEquals(expected, Utils.getNavTree());
    }

}
