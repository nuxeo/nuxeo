/*
 * (C) Copyright 2014-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Maxime Hilaire
 *     Florent Guillaume
 */
package org.nuxeo.ecm.directory.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.LoginService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;

@RunWith(FeaturesRunner.class)
@Features(CoreDirectoryFeature.class)
public class TestCoreDirectory {

    @Inject
    @Named(value = CoreDirectoryFeature.CORE_DIRECTORY_NAME)
    protected Directory coreDir;

    @Inject
    protected LoginService loginService;

    @Inject
    protected CoreFeature coreFeature;

    protected final static String SCHEMA_NAME = "schema1";

    protected final static String USER_SCHEMA_NAME = "user";

    protected final static String PREFIX_SCHEMA = "sch1";

    protected final static String USERNAME_FIELD = "username";

    protected final static String PASSWORD_FIELD = "password";

    protected final static String COMPANY_FIELD = "company";

    protected final static String UID_FIELD = PREFIX_SCHEMA + ":" + "uid";

    protected final static String BAR_FIELD = PREFIX_SCHEMA + ":" + "bar";

    protected final static String FOO_FIELD = PREFIX_SCHEMA + ":" + "foo";

    protected Session dirSession = null;

    @Before
    public void setUp() throws Exception {
        // be sure we don't retrieve a leaked security context
        Framework.login();
        dirSession = coreDir.getSession();

    }

    @After
    public void tearDown() throws Exception {
        dirSession.close();
    }

    @Test
    public void testCreateEntry() throws Exception {
        Map<String, Object> e;

        e = new HashMap<String, Object>();
        e.put(USERNAME_FIELD, "2");
        e.put(PASSWORD_FIELD, "foo3");
        e.put(COMPANY_FIELD, "bar3");
        DocumentModel doc = dirSession.createEntry(e);

        assertEquals("bar3", doc.getPropertyValue(BAR_FIELD));

    }

    @Test
    @Ignore
    public void testUpdateEntry() throws Exception {
        // TODO either fix mapping or remove mapping to fix this test
        // TODO test with different user's right

        Map<String, Object> e;
        e = new HashMap<String, Object>();
        e.put(USERNAME_FIELD, CoreDirectoryInit.DOC_ID_USER1);
        e.put(PASSWORD_FIELD, "foo3");
        e.put(COMPANY_FIELD, "bar3");

        DocumentModel docModel = dirSession.getEntry(CoreDirectoryInit.DOC_ID_USER1);
        docModel.setProperties(USER_SCHEMA_NAME, e);

        dirSession.updateEntry(docModel);

        docModel = dirSession.getEntry(CoreDirectoryInit.DOC_ID_USER1);
        Assert.assertEquals("foo3", docModel.getPropertyValue(FOO_FIELD));
    }

    @Test
    public void testAuthenticate() throws Exception {
        Assert.assertTrue(dirSession.authenticate(CoreDirectoryInit.DOC_ID_USER1, CoreDirectoryInit.DOC_PWD_USER1));
        Assert.assertFalse(dirSession.authenticate(CoreDirectoryInit.DOC_ID_USER1, "bad-pwd"));
        Assert.assertFalse(dirSession.authenticate("bad-id", "haha"));
        Assert.assertTrue(
                dirSession.authenticate(CoreDirectoryInit.DOC_ID_USERSHA1, CoreDirectoryInit.DOC_PWD_USERSHA1));
        Assert.assertFalse(
                dirSession.authenticate(CoreDirectoryInit.DOC_ID_USERSHA1, CoreDirectoryInit.DOC_PWD_BADPWDSHA1));
        // null password (avoid NPE)
        Assert.assertFalse(dirSession.authenticate(CoreDirectoryInit.DOC_ID_USERSHA1, null));
    }

    @Test
    public void testDeleteEntry() throws Exception {
        dirSession.deleteEntry("no-such-entry");
        dirSession.deleteEntry("1");
        assertNull(dirSession.getEntry("1"));
    }

    @Test
    public void testHasEntry() throws Exception {
        assertTrue(dirSession.hasEntry(CoreDirectoryInit.DOC_ID_USER1));
        assertFalse(dirSession.hasEntry("bad-id"));
    }

    @Test
    public void testQuery() throws Exception {

    }

    // TODO to be tested :
    // create an entry that already exist but the user has not permission to see
    // it
    // See where it is stored (if ok)
    // try to getEntry id

    @Test
    @Ignore
    public void testCreateFromModel() throws Exception {
        DocumentModel entry = BaseSession.createEntryModel(null, SCHEMA_NAME, null, null);
        String id = "newId";
        entry.setPropertyValue(UID_FIELD, id);

        assertNull(dirSession.getEntry(id));
        DocumentModel newDoc = dirSession.createEntry(entry);
        dirSession.updateEntry(newDoc);
        assertNotNull(dirSession.getEntry(id));

        // create one with existing same id, must fail
        entry.setProperty(USER_SCHEMA_NAME, USERNAME_FIELD, CoreDirectoryInit.DOC_ID_USER1);
        try {
            entry = dirSession.createEntry(entry);
            fail("Should raise an error, entry already exists");
        } catch (DirectoryException e) {
        }
    }

    @Test
    public void queryWithFilter() {
        Map<String, Serializable> usernamefilter = ImmutableMap.<String, Serializable> builder() //
                                                               .put("username", CoreDirectoryInit.DOC_ID_USER1) //
                                                               .build();

        DocumentModelList users = dirSession.query(usernamefilter);
        assertEquals(1, users.size());

        coreFeature.getStorageConfiguration().sleepForFulltext();

        Set<String> fulltext = new HashSet<>();
        fulltext.add("username");
        users = dirSession.query(usernamefilter, fulltext);
        assertEquals(1, users.size());
    }

}
