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
 *     Florent Guillaume
 *
 * $Id: TestMultiDirectory.java 30378 2008-02-20 17:37:26Z gracinet $
 */

package com.nuxeo.ecm.usersettings.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.usersettings.UserSettingsProvider;
import org.nuxeo.ecm.usersettings.UserSettingsService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:christophe.capon@vilogia.fr">Christophe Capon</a>
 */

@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy( {
        "org.nuxeo.ecm.usersettings.core", "org.nuxeo.ecm.usersettings.api",
        "org.nuxeo.ecm.usersettings.core.test", 
        "org.nuxeo.ecm.platform.userworkspace.api",
        "org.nuxeo.ecm.platform.userworkspace.types",
        "org.nuxeo.ecm.platform.userworkspace.core" })
public class TestUserSettings {

    @Inject
    CoreSession session;

    @Inject
    UserSettingsService service;

    @Inject
    RuntimeFeature runtime;

    @Test
    public void testRegisterProvider() throws Exception {
        assertEquals(2, service.getAllRegisteredProviders().size());
    }

    @Test
    public void testGetAllCurrentUserSettings() throws Exception {
        getAllCurrentUserSettings();
        getAllCurrentUserSettings();
    }

    private void getAllCurrentUserSettings() throws ClientException {
        UserSettingsService service = Framework.getLocalService(UserSettingsService.class);
        DocumentModelList docs = service.getCurrentUserSettings(session);
        assertEquals(2, docs.size());
        Map<String, DocumentModel> sets = new HashMap<String, DocumentModel>();
        Iterator<DocumentModel> it = docs.iterator();
        while (it.hasNext()) {
            DocumentModel doc = it.next();
            sets.put(doc.getType(), doc);
        }
        assertTrue(sets.containsKey("TestSettings1"));
        assertTrue(sets.containsKey("TestSettings2"));
        assertTrue(sets.get("TestSettings1").hasSchema("testsettings1"));
        assertTrue(sets.get("TestSettings2").hasSchema("testsettings2"));
        assertTrue(((String) (sets.get("TestSettings1").getProperty(
                "testsettings1", "testprop1"))).equalsIgnoreCase(SettingsProvider1.TEST_PROPERTY_VALUE));
        assertTrue(((String) (sets.get("TestSettings2").getProperty(
                "testsettings2", "testprop2"))).equalsIgnoreCase(SettingsProvider2.TEST_PROPERTY_VALUE));
    }

    @Test
    public void testGetAllCurrentUserSettingsFilterTypeName() throws Exception {

        getAllCurrentUserSettingsFilterTypeName();
        getAllCurrentUserSettingsFilterTypeName();

    }

    private void getAllCurrentUserSettingsFilterTypeName()
            throws ClientException {
        DocumentModelList docs = service.getCurrentUserSettings(session,
                "TestSettings1");
        assertEquals(1, docs.size());
        Map<String, DocumentModel> sets = new HashMap<String, DocumentModel>();
        Iterator<DocumentModel> it = docs.iterator();
        while (it.hasNext()) {
            DocumentModel doc = it.next();
            sets.put(doc.getType(), doc);
        }
        assertTrue(sets.containsKey("TestSettings1"));
        assertTrue(sets.get("TestSettings1").hasSchema("testsettings1"));
        assertTrue(((String) (sets.get("TestSettings1").getProperty(
                "testsettings1", "testprop1"))).equalsIgnoreCase(SettingsProvider1.TEST_PROPERTY_VALUE));
    }

    @Test
    public void testGetAllUserSettingsFilterTypeName() throws Exception {

        getAllUserSettingsFilterTypeName();
        getAllUserSettingsFilterTypeName();

    }

    private void getAllUserSettingsFilterTypeName() throws ClientException {
        DocumentModelList docs = service.getUserSettings(session,
                "Administrator", "TestSettings1");
        assertEquals(1, docs.size());
        Map<String, DocumentModel> sets = new HashMap<String, DocumentModel>();
        Iterator<DocumentModel> it = docs.iterator();
        while (it.hasNext()) {
            DocumentModel doc = it.next();
            sets.put(doc.getType(), doc);
        }
        assertTrue(sets.containsKey("TestSettings1"));
        assertTrue(sets.get("TestSettings1").hasSchema("testsettings1"));
        assertTrue(((String) (sets.get("TestSettings1").getProperty(
                "testsettings1", "testprop1"))).equalsIgnoreCase(SettingsProvider1.TEST_PROPERTY_VALUE));
    }

    @Test
    public void testGetAllCurrentUserSettingsFilterType() throws Exception {

        getAllCurrentUserSettingsFilterType();
        getAllCurrentUserSettingsFilterType();

    }

    private void getAllCurrentUserSettingsFilterType() throws ClientException {
        DocumentType dt = session.getDocumentType("TestSettings1");
        assertNotNull(dt);
        DocumentModelList docs = service.getCurrentUserSettings(session, dt);
        assertEquals(1, docs.size());
        Map<String, DocumentModel> sets = new HashMap<String, DocumentModel>();
        Iterator<DocumentModel> it = docs.iterator();
        while (it.hasNext()) {
            DocumentModel doc = it.next();
            sets.put(doc.getType(), doc);
        }
        assertTrue(sets.containsKey("TestSettings1"));
        assertTrue(sets.get("TestSettings1").hasSchema("testsettings1"));
        assertTrue(((String) (sets.get("TestSettings1").getProperty(
                "testsettings1", "testprop1"))).equalsIgnoreCase(SettingsProvider1.TEST_PROPERTY_VALUE));
    }

    @Test
    public void testGetAllUserSettingsFilterType() throws Exception {

        getAllUserSettingsFilterType();
        getAllUserSettingsFilterType();

    }

    private void getAllUserSettingsFilterType() throws ClientException {
        DocumentType dt = session.getDocumentType("TestSettings1");
        assertNotNull(dt);
        DocumentModelList docs = service.getUserSettings(session,
                "Administrator", dt);
        assertEquals(1, docs.size());
        Map<String, DocumentModel> sets = new HashMap<String, DocumentModel>();
        Iterator<DocumentModel> it = docs.iterator();
        while (it.hasNext()) {
            DocumentModel doc = it.next();
            sets.put(doc.getType(), doc);
        }
        assertTrue(sets.containsKey("TestSettings1"));
        assertTrue(sets.get("TestSettings1").hasSchema("testsettings1"));
        assertTrue(((String) (sets.get("TestSettings1").getProperty(
                "testsettings1", "testprop1"))).equalsIgnoreCase(SettingsProvider1.TEST_PROPERTY_VALUE));
    }

    @Test
    public void testGetRootUserSettingsFilterTypeName() throws Exception {

        getRootUserSettingsFilterTypeName();
        getRootUserSettingsFilterTypeName();

    }

    private void getRootUserSettingsFilterTypeName() throws ClientException {
        DocumentModelList docs = service.getUserSettings(session,
                "Administrator", "UserSettings");
        assertEquals(1, docs.size());
        assertEquals("UserSettings", docs.get(0).getType());
    }


    @Test
    public void getEmptyList() throws ClientException {
        service.clearProviders();
        DocumentModelList docs = service.getUserSettings(session,
                "Administrator", "TestSettings1");
        assertEquals(0, docs.size());
    }


    @Test
    public void duplicateRegister() throws ClientException {
        ensureRegistered();
        Entry<String, UserSettingsProvider> item = service.getAllRegisteredProviders().entrySet().iterator().next();
        service.registerProvider(item.getKey(), item.getValue());
    }

    private void ensureRegistered() throws ClientException {
        if (!service.getAllRegisteredProviders().containsKey("SettingsProvider1")) {
            service.registerProvider("SettingsProvider1", new SettingsProvider1());
        }
        if (!service.getAllRegisteredProviders().containsKey("SettingsProvider2")) {
            service.registerProvider("SettingsProvider2", new SettingsProvider2());
        }
        assertEquals(2, service.getAllRegisteredProviders().size());
    }

    @Test
    public void nonExistentUnregister() throws ClientException {
        service.unRegisterProvider("dummy");
    }

    @Test
    public void errorUserWorkspace() throws ClientException {
        service.getUserSettings(session, "dummyUserName");
    }


}
