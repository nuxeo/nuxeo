/*
 * (C) Copyright 2006-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Christophe Capon
 *
 */

package com.nuxeo.ecm.usersettings.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.usersettings.UserSettingsType;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:christophe.capon@vilogia.fr">Christophe Capon</a>
 */

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy( { 
    "org.nuxeo.ecm.usersettings.core", "org.nuxeo.ecm.usersettings.api",
    "org.nuxeo.ecm.usersettings.core.test", 
    "org.nuxeo.ecm.platform.content.template"})
public class TestUserSettingsTypes {

    @Inject
    CoreSession session;

    private static final String TEST_USERNAME1 = "testusername";
    private static final String TEST_USERNAME2 = "testusername2";
    private static final String DEFAULT_DOMAIN = "/default-domain";

    @Test
    public void testCreateUserSettingsDocument() throws Exception {
        DocumentModel doc = createUserSettingsDoc();
        Object o = doc.getProperty(UserSettingsType.SCHEMA,
                UserSettingsType.PROP_USER);
        assertNotNull(o);
        assertTrue(o instanceof String);
        assertEquals(TEST_USERNAME1, (String) o);
    }

    private DocumentModel createUserSettingsDoc() throws ClientException {

        DocumentModel root = session.getDocument(new PathRef(DEFAULT_DOMAIN));
        DocumentModel testDoc = session.createDocumentModel(
                root.getPathAsString(), UserSettingsType.DOCTYPE + "-instance",
                UserSettingsType.DOCTYPE);
        assertNotNull(testDoc);
        testDoc.setProperty(UserSettingsType.SCHEMA,
                UserSettingsType.PROP_USER, TEST_USERNAME1);
        testDoc = session.createDocument(testDoc);
        session.saveDocument(testDoc);
        return testDoc;
    }

}
