package org.nuxeo.opensocial.container.server.webcontent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;

public class OpenSocialAdapterRepositoryInit implements RepositoryInit {

    @Override
    public void populate(CoreSession session) throws ClientException {
        DocumentModel wco1 = session.createDocumentModel("/", "wco1", "WCOpenSocial");
        List<Map<String, Serializable>> prefs = new ArrayList<Map<String, Serializable>>();
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        map.put("name", "pref1");
        map.put("value", "val1");
        prefs.add(map);
        wco1.setPropertyValue("wcopensocial:userPrefs", (Serializable) prefs);
        wco1.setPropertyValue("wcopensocial:gadgetDefUrl", "bla");
        wco1 = session.createDocument(wco1);
        session.save();
        // TODO Auto-generated method stub
        
    }

}
