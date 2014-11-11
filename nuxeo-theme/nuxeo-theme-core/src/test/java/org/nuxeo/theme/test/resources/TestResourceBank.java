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

package org.nuxeo.theme.test.resources;

import java.io.IOException;
import java.util.Map;

import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.resources.BankManager;
import org.nuxeo.theme.resources.BankUtils;
import org.nuxeo.theme.resources.ResourceBank;
import org.nuxeo.theme.themes.ThemeManager;
import org.nuxeo.theme.types.TypeRegistry;

public class TestResourceBank extends NXRuntimeTestCase {

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
        deployContrib("org.nuxeo.theme.core.tests", "theme-bank-config.xml");
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

    public void testGetRegisteredBanks() {
        ResourceBank bank = ThemeManager.getResourceBank(BANK_NAME);
        assertEquals("test", bank.getName());
        assertEquals("http://localhost:8080/nuxeo/site/theme-banks/test",
                bank.getConnectionUrl());
    }

    public void testGetBankNames() throws IOException {
        assertEquals(BANK_NAME, BankManager.getBankNames().get(0));
    }

    public void testGetCollections() throws IOException {
        assertEquals(COLLECTION_NAME,
                BankManager.getCollections(BANK_NAME).get(0));
        assertEquals(COLLECTION_NAME,
                BankManager.getCollections(BANK_NAME).get(0));
        assertEquals(COLLECTION_NAME,
                BankManager.getCollections(BANK_NAME).get(0));
    }

    public void testGetBankLogoFile() throws IOException {
        assertTrue(BankManager.getBankLogoFile(BANK_NAME).getPath().endsWith(
                "/test/logo.png"));
    }

    public void testGetImageFile() throws IOException {
        assertTrue(BankManager.getImageFile(BANK_NAME, COLLECTION_NAME,
                "emoticon_smile.png").getPath().endsWith(
                "/test/Test/image/emoticon_smile.png"));
    }

    public void testGetStyleInfo() throws IOException {
        Map styleInfo = (Map) BankManager.getInfo(BANK_NAME, COLLECTION_NAME,
                "style").get("test.css");
        assertEquals(styleInfo.get("description"), "Test skin");
        assertEquals(styleInfo.get("skin"), true);
    }

    public void testGetStylePreviewFile() throws IOException {
        assertEquals(
                "test.png",
                BankManager.getStylePreviewFile(BANK_NAME, COLLECTION_NAME,
                        "test.css").getName());
        assertEquals(
                "style-preview.png",
                BankManager.getStylePreviewFile(BANK_NAME, COLLECTION_NAME,
                        "style-with-preview.css").getName());
    }

    public void testGetStylePreviewFileNotFound() throws IOException {

        boolean thrown = false;
        try {
            BankManager.getStylePreviewFile(BANK_NAME, COLLECTION_NAME,
                    "style-not-found.css");
        } catch (IOException e) {
            thrown = true;
        }
        assertTrue(thrown);

        thrown = false;
        try {
            BankManager.getStylePreviewFile(BANK_NAME, COLLECTION_NAME,
                    "skin-without-preview.css");
        } catch (IOException e) {
            thrown = true;
        }
        assertTrue(thrown);

        thrown = false;
        try {
            BankManager.getStylePreviewFile(BANK_NAME, COLLECTION_NAME,
                    "style-without-preview.css");
        } catch (IOException e) {
            thrown = true;
        }
        assertTrue(thrown);

    }

    public void testCheckFilePath() {
        assertTrue(BankUtils.checkFilePath("test.css"));
        assertTrue(BankUtils.checkFilePath("/test/test.css"));
        assertTrue(BankUtils.checkFilePath("/test-1"));
        assertTrue(BankUtils.checkFilePath("/a-b/test-1"));
        assertTrue(BankUtils.checkFilePath("/a b/test-1"));
        assertTrue(BankUtils.checkFilePath("ab"));
        assertTrue(BankUtils.checkFilePath("a"));

        assertFalse(BankUtils.checkFilePath("../test/test.css"));
        assertFalse(BankUtils.checkFilePath("/test/../test/test.css"));
        assertFalse(BankUtils.checkFilePath("/test/./test/test.css"));
        assertFalse(BankUtils.checkFilePath("/test/ /test/test.css"));
        assertFalse(BankUtils.checkFilePath("/test/a /test/test.css"));
        assertFalse(BankUtils.checkFilePath("/test/ b/test/test.css"));
        assertFalse(BankUtils.checkFilePath("./test/test/test.css"));
        assertFalse(BankUtils.checkFilePath("/test/test/."));
        assertFalse(BankUtils.checkFilePath("/test/test/.."));
        assertFalse(BankUtils.checkFilePath("/test/test/../"));
        assertFalse(BankUtils.checkFilePath("\\test\\..\\"));
        assertFalse(BankUtils.checkFilePath("test*"));
        assertFalse(BankUtils.checkFilePath("test:"));
        assertFalse(BankUtils.checkFilePath("test>"));
        assertFalse(BankUtils.checkFilePath("test<"));
        assertFalse(BankUtils.checkFilePath("test?"));
        assertFalse(BankUtils.checkFilePath("test\""));
        assertFalse(BankUtils.checkFilePath("test'a"));
        assertFalse(BankUtils.checkFilePath("test\ta"));
        assertFalse(BankUtils.checkFilePath("test\na"));
    }
}
