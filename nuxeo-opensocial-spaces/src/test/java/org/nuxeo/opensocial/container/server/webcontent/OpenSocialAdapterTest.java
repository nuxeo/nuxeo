package org.nuxeo.opensocial.container.server.webcontent;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.opensocial.container.OpenSocialContainerFeature;
import org.nuxeo.opensocial.container.server.webcontent.OpenSocialAdapterRepositoryInit;
import org.nuxeo.opensocial.container.server.webcontent.api.WebContentAdapter;
import org.nuxeo.opensocial.container.server.webcontent.gadgets.opensocial.OpenSocialAdapter;
import org.nuxeo.opensocial.container.shared.webcontent.OpenSocialData;
import org.nuxeo.opensocial.container.shared.webcontent.UserPref;
import org.nuxeo.opensocial.container.shared.webcontent.enume.DataType;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(OpenSocialContainerFeature.class)
@RepositoryConfig(init=OpenSocialAdapterRepositoryInit.class)
public class OpenSocialAdapterTest {

    @Inject
    CoreSession session;

    @Test
    public void iCanGetUserPreferences() throws Exception {
        DocumentModel wco = session.getDocument(new PathRef("/wco1"));
        assertNotNull(wco);
        List<Map<String, Serializable>> prefs = (List<Map<String, Serializable>>) wco.getPropertyValue("wcopensocial:userPrefs");
        assertNotNull(prefs);
        assertFalse(prefs.isEmpty());
        assertEquals(1, prefs.size());
        Map<String, Serializable> pref1 = prefs.get(0);
        assertEquals("pref1", pref1.get("name"));
        assertEquals("val1", pref1.get("value"));
    }
    
    @Test
    public void iCanFeedDataWithExistingUserPref() throws Exception {
        OpenSocialData data = new OpenSocialData();
        assertNotNull(data);
        data.setGadgetDef("bla");
        List<UserPref> userPrefs = new ArrayList<UserPref>();
        UserPref pref = new UserPref("pref1", DataType.STRING);
        pref.setActualValue("val1");
        userPrefs.add(pref);
        data.setUserPrefs(userPrefs);
        DocumentModel wco = session.getDocument(new PathRef("/wco1"));
        OpenSocialAdapter adapter = (OpenSocialAdapter) wco.getAdapter(WebContentAdapter.class);
        assertNotNull(adapter);
        adapter.feedFrom(data);
        session.saveDocument(wco);
        session.save();
        List<Map<String, Serializable>> prefs = (List<Map<String, Serializable>>) wco.getPropertyValue("wcopensocial:userPrefs");
        assertNotNull(prefs);
        assertEquals(1, prefs.size());
        Map<String, Serializable> pref1 = prefs.get(0);
        assertEquals("pref1", pref1.get("name"));
        assertEquals("val1", pref1.get("value"));
    }
    
    @Test
    public void iCanModifyUserPrefValue() throws Exception {
        OpenSocialData data = new OpenSocialData();
        assertNotNull(data);
        data.setGadgetDef("bla");
        List<UserPref> userPrefs = new ArrayList<UserPref>();
        UserPref pref = new UserPref("pref1", DataType.STRING);
        pref.setActualValue("val0");
        userPrefs.add(pref);
        data.setUserPrefs(userPrefs);
        DocumentModel wco = session.getDocument(new PathRef("/wco1"));
        OpenSocialAdapter adapter = (OpenSocialAdapter) wco.getAdapter(WebContentAdapter.class);
        assertNotNull(adapter);
        adapter.feedFrom(data);
        session.saveDocument(wco);
        session.save();
        List<Map<String, Serializable>> prefs = (List<Map<String, Serializable>>) wco.getPropertyValue("wcopensocial:userPrefs");
        assertNotNull(prefs);
        assertEquals(1, prefs.size());
        Map<String, Serializable> pref1 = prefs.get(0);
        assertEquals("pref1", pref1.get("name"));
        assertEquals("val0", pref1.get("value"));
    }

    @Test
    public void iCanFeedDataWithNewUserPref() throws Exception {
        OpenSocialData data = new OpenSocialData();
        assertNotNull(data);
        data.setGadgetDef("bla");
        List<UserPref> userPrefs = new ArrayList<UserPref>();
        UserPref userPref = new UserPref("pref2", DataType.STRING);
        userPref.setActualValue("val2");
        userPrefs.add(userPref);
        data.setUserPrefs(userPrefs);
        DocumentModel wco = session.getDocument(new PathRef("/wco1"));
        OpenSocialAdapter adapter = (OpenSocialAdapter) wco.getAdapter(WebContentAdapter.class);
        assertNotNull(adapter);
        adapter.feedFrom(data);
        session.saveDocument(wco);
        session.save();
        List<Map<String, Serializable>> prefs = (List<Map<String, Serializable>>) wco.getPropertyValue("wcopensocial:userPrefs");
        assertNotNull(prefs);
        assertEquals(2, prefs.size());
        Map<String, Serializable> pref = prefs.get(0);
        assertEquals("pref1", pref.get("name"));
        assertEquals("val0", pref.get("value"));
        pref = prefs.get(1);
        assertEquals("pref2", pref.get("name"));
        assertEquals("val2", pref.get("value"));
    }
}
