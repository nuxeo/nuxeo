/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.user.preferences.directory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.EVERYTHING;
import static org.nuxeo.user.preferences.api.UserPreferencesUtil.newUserDocPreferences;
import static org.nuxeo.user.preferences.directory.UserPreferencesServiceImpl.withDirectorySession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.MultiRepositoryFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;
import org.nuxeo.user.preferences.api.UserPreference;
import org.nuxeo.user.preferences.api.UserPreferencesService;
import org.nuxeo.user.preferences.api.UserPreferencesUtil;
import org.nuxeo.user.preferences.exception.InvalidUserPreferencesKey;
import org.nuxeo.user.preferences.exception.TooManyUserPreferencesException;
import org.nuxeo.user.preferences.exception.UserPreferencesExistsException;
import org.nuxeo.user.preferences.exception.UserPreferencesNotFound;

/**
 * @since 2025.16
 */
@RunWith(FeaturesRunner.class)
@Features({ UserPreferencesFeature.class, MultiRepositoryFeature.class })
public class TestUserPreferencesService {

    protected final static String JACK_USER = "Jack";

    protected final static String JOHN_USER = "John";

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected CoreSession session;

    @Inject
    @Named("other")
    protected CoreSession otherSession;

    @Inject
    protected UserPreferencesService ups;

    protected DocumentModel doc;

    protected void addEverythingPermission(DocumentModel documentModel, String username) {
        ACP acp = documentModel.getACP();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE(username, EVERYTHING, true));
        session.setACP(documentModel.getRef(), acp, false);
        session.save();
    }

    protected void assertNoUserPreferences() {
        assertEquals(0, numberOfUserPreferences());
    }

    @SuppressWarnings("deprecation")
    protected int numberOfUserPreferences() {
        return withDirectorySession(dirSession -> {
            return dirSession.query(new QueryBuilder()).size();
        });
    }

    @Before
    public void setup() {
        doc = session.createDocumentModel("/", "myDoc", "File");
        doc = session.createDocument(doc);
        txFeature.nextTransaction();
    }

    @Test
    public void testCreateUserPreference() {
        assertTrue(ups.get(session, "foo").isEmpty());
        var pref = ups.create(session, "foo", "bar");
        assertEquals("foo", pref.key());
        assertEquals("bar", pref.value());
        txFeature.nextTransaction();
        var optPref = ups.get(session, "foo");
        assertTrue(optPref.isPresent());
        assertEquals("foo", optPref.get().key());
        assertEquals("bar", optPref.get().value());
        assertEquals(1, ups.list(session).size());

        pref = ups.createOrUpdate(session, "foo1", "truc1");
        assertEquals("truc1", pref.value());
        txFeature.nextTransaction();
        optPref = ups.get(session, "foo1");
        assertTrue(optPref.isPresent());
        assertEquals("foo1", optPref.get().key());
        assertEquals("truc1", optPref.get().value());
        assertEquals(2, ups.list(session).size());
    }

    @Test
    public void testUpdateUserPreference() {
        assertThrows(UserPreferencesNotFound.class, () -> ups.update(session, "missing", "dummy"));

        assertTrue(ups.get(session, "foo").isEmpty());
        var pref = ups.create(session, "foo", "bar");
        txFeature.nextTransaction();

        pref = ups.update(session, pref.key(), "otherValue");
        assertEquals("otherValue", pref.value());
        txFeature.nextTransaction();
        var optPref = ups.get(session, "foo");
        assertTrue(optPref.isPresent());
        assertEquals("foo", optPref.get().key());
        assertEquals("otherValue", optPref.get().value());

        pref = ups.createOrUpdate(session, "foo", "yetAnotherValue");
        assertEquals("yetAnotherValue", pref.value());
        txFeature.nextTransaction();
        optPref = ups.get(session, "foo");
        assertTrue(optPref.isPresent());
        assertEquals("foo", optPref.get().key());
        assertEquals("yetAnotherValue", optPref.get().value());
    }

    @Test
    public void testRemoveUserPreference() {
        ups.create(session, "foo", "bar");
        txFeature.nextTransaction();

        assertEquals(1, ups.list(session).size());
        ups.delete(session, "foo");
        txFeature.nextTransaction();
        var optPref = ups.get(session, "foo1");
        assertFalse(optPref.isPresent());
        assertEquals(0, ups.list(session).size());
    }

    @Test
    public void testCreateDocUserPreferences() {
        assertTrue(ups.get(session, doc.getRef()).isEmpty());
        var prefs = ups.create(session, doc.getRef(), Map.of("key1", "value1", "key2", "value2"));
        assertEquals("value1", prefs.getPreference("key1"));
        assertEquals("value2", prefs.getPreference("key2"));
        assertNull(prefs.getPreference("unknown"));
        txFeature.nextTransaction();
        prefs = ups.get(session, doc.getRef());
        assertEquals("value1", prefs.getPreference("key1"));
        assertEquals("value2", prefs.getPreference("key2"));
        assertNull(prefs.getPreference("unknown"));
        var optPref = ups.get(session, doc.getRef(), "key1");
        assertTrue(optPref.isPresent());
        assertEquals("value1", optPref.get().value());
        optPref = ups.get(session, doc.getRef(), "key2");
        assertTrue(optPref.isPresent());
        assertEquals("value2", optPref.get().value());
        optPref = ups.get(session, doc.getRef(), "unknown");
        assertTrue(optPref.isEmpty());
    }

    @Test
    public void testPutAllDocUserPreferences() {
        var prefs = ups.create(session, doc.getRef(), Map.of("foo", "bar"));
        prefs = ups.putAll(session, doc.getRef(), newUserDocPreferences("foo", "truc"));
        assertEquals("truc", prefs.getPreference("foo"));
        txFeature.nextTransaction();
        prefs = ups.get(session, doc.getRef());
        assertEquals("truc", prefs.getPreference("foo"));

        ups.putAll(session, doc.getRef(), newUserDocPreferences(Map.of("key1", "value1", "key2", "value2")));
        txFeature.nextTransaction();
        prefs = ups.get(session, doc.getRef());
        assertEquals(3, prefs.size());
        assertEquals("truc", prefs.getPreference("foo"));
        assertEquals("value1", prefs.getPreference("key1"));
        assertEquals("value2", prefs.getPreference("key2"));

        prefs = ups.putAll(session, doc.getRef(), newUserDocPreferences("key2", "otherValue2"));
        assertEquals(3, prefs.size());

        prefs = ups.putAll(session, doc.getRef(), newUserDocPreferences("key3", "value3"));
        assertEquals(4, prefs.size());
        assertEquals("truc", prefs.getPreference("foo"));
        assertEquals("value1", prefs.getPreference("key1"));
        assertEquals("otherValue2", prefs.getPreference("key2"));
        assertEquals("value3", prefs.getPreference("key3"));
    }

    @Test
    public void testUpdateDocUserPreferences() {
        assertThrows(UserPreferencesNotFound.class,
                () -> ups.update(session, doc.getRef(), newUserDocPreferences("foo", "bar")));

        var prefs = ups.create(session, doc.getRef(), Map.of("key1", "value1", "key2", "value2"));
        prefs = ups.update(session, doc.getRef(), newUserDocPreferences(Map.of("a", "a", "b", "b")));
        assertEquals(2, prefs.size());
        assertEquals("a", prefs.getPreference("a"));
        assertEquals("b", prefs.getPreference("b"));
        txFeature.nextTransaction();

        prefs = ups.get(session, doc.getRef());
        assertEquals(2, prefs.size());
        assertEquals("a", prefs.getPreference("a"));
        assertEquals("b", prefs.getPreference("b"));
        txFeature.nextTransaction();
    }

    @Test
    public void testRemoveDocUserPreferences() {
        var prefs = ups.create(session, doc.getRef(), Map.of("a", "a", "b", "b"));
        // Remove key
        prefs = ups.remove(session, doc.getRef(), "a");
        assertEquals(1, prefs.size());
        assertEquals("b", prefs.getPreference("b"));
        txFeature.nextTransaction();

        // Remove key of missing preferences does nothing
        assertEquals(prefs.preferences(), ups.remove(session, doc.getRef(), "foo").preferences());

        // Remove last remaining key returns empty preferences
        var updated = ups.remove(session, doc.getRef(), "b");
        assertTrue(updated.preferences().isEmpty());
        txFeature.nextTransaction();
        assertTrue(ups.get(session, doc.getRef()).isEmpty());

        // Remove
        ups.create(session, doc.getRef(), Map.of("foo", "bar"));
        ups.delete(session, doc.getRef());
        txFeature.nextTransaction();
        prefs = ups.get(session, doc.getRef());
        assertTrue(prefs.isEmpty());
    }

    @Test
    @SuppressWarnings("DataFlowIssue") // For test against null values
    public void shouldValidatePreferenceKeys() {
        // User Preference
        assertThrows(NullPointerException.class, () -> ups.create(session, null, "bar"));
        assertThrows(InvalidUserPreferencesKey.class, () -> ups.create(session, "!foo", "bar"));
        assertThrows(InvalidUserPreferencesKey.class, () -> ups.create(session, "", "bar"));
        assertThrows(InvalidUserPreferencesKey.class, () -> ups.create(session, "\"", "bar"));
        assertThrows(NullPointerException.class, () -> ups.createOrUpdate(session, null, "bar"));
        assertThrows(InvalidUserPreferencesKey.class, () -> ups.createOrUpdate(session, "!foo", "bar"));
        var tooLongKey = "WaaaaaaaaaaayyyyyyyyyyToooooooooooooooooooooooLooooooooooooooooonnnnnnnnnnnngKeeeeeeeeeeeeeeeeeeeeyyy";
        assertThrows(InvalidUserPreferencesKey.class, () -> ups.create(session, tooLongKey, "bar"));

        // User Doc Preferences
        assertThrows(InvalidUserPreferencesKey.class, () -> ups.create(session, doc.getRef(), Map.of("<foo", "bar")));
        assertThrows(InvalidUserPreferencesKey.class, () -> ups.create(session, doc.getRef(), Map.of("", "bar")));
        assertThrows(InvalidUserPreferencesKey.class,
                () -> ups.create(session, doc.getRef(), Map.of(tooLongKey, "bar")));
        assertThrows(InvalidUserPreferencesKey.class, () -> ups.create(session, tooLongKey, "bar"));
        ups.create(session, doc.getRef(), Map.of("foo", "bar"));
        txFeature.nextTransaction();
        var invalidPreferences = newUserDocPreferences("!", "bar");
        assertThrows(InvalidUserPreferencesKey.class, () -> ups.putAll(session, doc.getRef(), invalidPreferences));
        assertThrows(InvalidUserPreferencesKey.class, () -> ups.update(session, doc.getRef(), invalidPreferences));
    }

    @Test
    @SuppressWarnings("DataFlowIssue") // For test against null values
    public void shouldNotAllowNullPreferences() {
        assertThrows(NullPointerException.class, () -> ups.create(session, "foo", null));
        assertThrows(NullPointerException.class, () -> ups.createOrUpdate(session, "foo", null));
        var pref = ups.create(session, "foo", "bar");
        txFeature.nextTransaction();
        assertThrows(NullPointerException.class, () -> ups.update(session, pref.key(), null));

        var map = new HashMap<String, String>();
        map.put("invalid", null);
        map.put("valid", "aValue");
        assertThrows(NullPointerException.class, () -> ups.create(session, doc.getRef(), map));
        assertThrows(NullPointerException.class, () -> ups.create(session, doc.getRef(), Map.of()));
        assertThrows(NullPointerException.class, () -> ups.create(session, doc.getRef(), null));
        ups.create(session, doc.getRef(), Map.of("foo", "bar"));
        txFeature.nextTransaction();
        assertThrows(NullPointerException.class,
                () -> ups.putAll(session, doc.getRef(), newUserDocPreferences("foo", null)));
        assertThrows(NullPointerException.class, () -> ups.putAll(session, doc.getRef(), newUserDocPreferences(map)));
        assertThrows(NullPointerException.class,
                () -> ups.putAll(session, doc.getRef(), newUserDocPreferences(Map.of())));
        assertThrows(NullPointerException.class,
                () -> ups.putAll(session, doc.getRef(), newUserDocPreferences((List<UserPreference>) null)));
        assertThrows(NullPointerException.class, () -> ups.update(session, doc.getRef(), newUserDocPreferences(map)));
        assertThrows(NullPointerException.class,
                () -> ups.update(session, doc.getRef(), newUserDocPreferences(Map.of())));
        assertThrows(NullPointerException.class,
                () -> ups.update(session, doc.getRef(), newUserDocPreferences((List<UserPreference>) null)));
    }

    @Test
    public void shouldNotCreatePreferencesOnMissingDoc() {
        assertThrows(DocumentNotFoundException.class,
                () -> ups.create(session, new PathRef("/missing"), Map.of("foo", "bar")));
    }

    @Test
    public void shouldNotCreateUserPreferenceTwice() {
        ups.create(session, "foo", "bar");
        txFeature.nextTransaction();
        assertThrows(UserPreferencesExistsException.class, () -> ups.create(session, "foo", "bar"));
    }

    @Test
    public void shouldNotCreateUserPreferencesTwice() {
        ups.create(session, doc.getRef(), Map.of("foo", "bar"));
        txFeature.nextTransaction();
        assertThrows(UserPreferencesExistsException.class,
                () -> ups.create(session, doc.getRef(), Map.of("foo", "bar")));
    }

    @Test
    @WithFrameworkProperty(name = UserPreferencesUtil.PROP_MAX_PREFERENCES, value = "2")
    public void shouldNotExceedPreferencesGlobalLimit() {
        // Creating beyond limit
        assertThrows(TooManyUserPreferencesException.class,
                () -> ups.create(session, doc.getRef(), Map.of("key1", "value1", "key2", "value2", "key3", "value3")));
        assertNoUserPreferences();

        // Create 2 preferences to reach limit
        ups.create(session, doc.getRef(), Map.of("key1", "value1"));
        ups.create(session, "machin", "truc");
        txFeature.nextTransaction();

        // Update works
        ups.update(session, "machin", "otherTruc");
        ups.update(session, doc.getRef(), newUserDocPreferences("key1", "otherValue1"));
        txFeature.nextTransaction();
        ups.putAll(session, doc.getRef(), newUserDocPreferences("key1", "yetAnotherValue1"));
        txFeature.nextTransaction();
        ups.putAll(session, doc.getRef(), newUserDocPreferences("key1", "basta"));
        txFeature.nextTransaction();

        // But we cannot create new ones
        assertThrows(TooManyUserPreferencesException.class, () -> ups.create(session, "foo", "bar"));
        assertThrows(TooManyUserPreferencesException.class,
                () -> ups.putAll(session, doc.getRef(), newUserDocPreferences("foo", "bar")));
        assertThrows(TooManyUserPreferencesException.class,
                () -> ups.update(session, doc.getRef(), newUserDocPreferences(Map.of("foo", "bar", "aKey", "aValue"))));

        assertEquals(2, numberOfUserPreferences());
    }

    @Test
    public void shouldGarbageCollectPreferencesAfterDocRemoved() {
        var root = session.getRootDocument();
        addEverythingPermission(root, JOHN_USER);
        addEverythingPermission(root, JACK_USER);
        txFeature.nextTransaction();

        // As John user, I create preferences on doc
        CoreSession johnSession = coreFeature.getCoreSession(JOHN_USER);
        ups.create(johnSession, doc.getRef(), Map.of("key1", "value1"));
        txFeature.nextTransaction();

        // As Jack user, I create preferences on doc
        CoreSession jackSession = coreFeature.getCoreSession(JACK_USER);
        ups.create(jackSession, doc.getRef(), Map.of("key2", "value2"));

        // As Admin, I create preferences on root
        ups.create(session, session.getRootDocument().getRef(), Map.of("key3", "value3"));
        txFeature.nextTransaction();
        // Jack + John + Admin preferences
        assertEquals(3, numberOfUserPreferences());

        // When John removes the doc
        johnSession.removeDocument(doc.getRef());
        txFeature.nextTransaction();
        // Only 1 preference left for Admin on root doc
        assertEquals(1, numberOfUserPreferences());
        assertEquals(1, ups.get(session, session.getRootDocument().getRef()).size());
    }

    @Test
    public void shouldCleanupPreferencesAfterUserDeleted() {
        // Create John User and add permission on default repository
        var userManager = Framework.getService(UserManager.class);
        DocumentModel userModel = userManager.getBareUserModel();
        userModel.setProperty("user", "username", JOHN_USER);
        userModel.setProperty("user", "password", JOHN_USER);
        userManager.createUser(userModel);
        addEverythingPermission(session.getRootDocument(), JOHN_USER);

        // As system, I create a preference in both default and other repository
        ups.create(session, "system", "1");
        ups.create(otherSession, "system", "2");
        assertEquals(2, numberOfUserPreferences());
        // As JOHN, I create a bunch of preferences in default repository
        CoreSession johnSession = coreFeature.getCoreSession(JOHN_USER);
        ups.create(johnSession, "foo", "truc");
        ups.create(johnSession, doc.getRef(), Map.of("key1", "value1"));
        // and in other repository
        johnSession = CoreInstance.getCoreSession("other", JOHN_USER);
        ups.create(johnSession, "otherFoo", "otherTruc");
        assertEquals(5, numberOfUserPreferences());

        // Delete John User and assert only his preferences are deleted
        userManager.deleteUser(JOHN_USER);
        txFeature.nextTransaction();
        assertEquals(2, numberOfUserPreferences());
        assertTrue(ups.get(session, "system").isPresent());
        assertTrue(ups.get(otherSession, "system").isPresent());
    }

    @Test
    public void testShouldNotMixPreferenceAndDocPreferencesKeys() {
        // Create a global preference
        ups.create(session, "foo", "value1");
        // Create a preference for a doc with the same key
        ups.create(session, doc.getRef(), Map.of("bar", "value1", "foo", "value2"));
        // Create a global preference with an existing doc preference key
        ups.create(session, "bar", "value2");
        txFeature.nextTransaction();

        var optPref = ups.get(session, "foo");
        assertTrue(optPref.isPresent());
        assertEquals("value1", optPref.get().value());
        optPref = ups.get(session, "bar");
        assertTrue(optPref.isPresent());
        assertEquals("value2", optPref.get().value());
        var prefs = ups.get(session, doc.getRef());
        assertEquals("value2", prefs.preferences().get("foo"));
        prefs = ups.get(session, doc.getRef());
        assertEquals("value1", prefs.preferences().get("bar"));
    }

    @Test
    public void testShouldIsolateKeysBetweenRepository() {
        // Create a global preference in default repository
        ups.create(session, "foo", "value1");
        // Create a global preference in other repository with same key
        ups.create(otherSession, "foo", "value2");

        var optPref = ups.get(session, "foo");
        assertTrue(optPref.isPresent());
        assertEquals("value1", optPref.get().value());
        optPref = ups.get(otherSession, "foo");
        assertTrue(optPref.isPresent());
        assertEquals("value2", optPref.get().value());
    }

    @Test
    public void shouldIsolateUserPreferences() {
        var root = session.getRootDocument();
        addEverythingPermission(root, JOHN_USER);
        addEverythingPermission(root, JACK_USER);
        txFeature.nextTransaction();

        // As John, I create a bunch of preferences
        CoreSession johnSession = coreFeature.getCoreSession(JOHN_USER);
        ups.create(johnSession, "foo", "truc");
        ups.create(johnSession, doc.getRef(), Map.of("key1", "value1"));
        txFeature.nextTransaction();
        assertTrue(ups.get(johnSession, "foo").isPresent());
        assertFalse(ups.get(johnSession, doc.getRef()).isEmpty());

        // As Jack, I cannot see John's preferences
        CoreSession jackSession = coreFeature.getCoreSession(JACK_USER);
        assertTrue(ups.get(jackSession, "foo").isEmpty());
        assertTrue(ups.get(jackSession, doc.getRef()).isEmpty());
    }

    @Test
    public void shouldSanitizeValues() {
        var xss = "foo<script>alert('XSS!!!')</script>bar";
        var pref = ups.create(session, "key", xss);
        assertEquals("foobar", pref.value());
        pref = ups.createOrUpdate(session, "otherKey", xss);
        assertEquals("foobar", pref.value());
        var prefs = ups.create(session, doc.getRef(), Map.of("key", xss));
        assertEquals("foobar", prefs.getPreference("key"));
        txFeature.nextTransaction();
        xss = "other<script>alert('XSS!!!')</script>Attempt";
        pref = ups.update(session, pref.key(), xss);
        assertEquals("otherAttempt", pref.value());
        prefs = ups.putAll(session, doc.getRef(), newUserDocPreferences("key", xss));
        assertEquals("otherAttempt", prefs.getPreference("key"));
    }

    @Test
    @WithFrameworkProperty(name = UserPreferencesUtil.PROP_SANITIZE_VALUES, value = "false")
    public void shouldNotSanitizeValues() {
        var xss = "foo<script>alert('XSS!!!')</script>bar";
        var pref = ups.create(session, "key", xss);
        assertEquals(xss, pref.value());
        var prefs = ups.create(session, doc.getRef(), Map.of("key", xss));
        assertEquals(xss, prefs.getPreference("key"));
        txFeature.nextTransaction();
        xss = "other<script>alert('XSS!!!')</script>Attempt";
        pref = ups.update(session, pref.key(), xss);
        assertEquals(xss, pref.value());
        prefs = ups.putAll(session, doc.getRef(), newUserDocPreferences("key", xss));
        assertEquals(xss, prefs.getPreference("key"));
    }

}
