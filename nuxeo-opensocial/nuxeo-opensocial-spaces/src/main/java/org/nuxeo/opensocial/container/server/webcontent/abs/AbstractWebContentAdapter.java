package org.nuxeo.opensocial.container.server.webcontent.abs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;
import org.nuxeo.opensocial.container.shared.webcontent.enume.DefaultPortletPreference;

/**
 * @author Stéphane Fourrier
 */
public abstract class AbstractWebContentAdapter {

    protected DocumentModel doc;

    public AbstractWebContentAdapter(DocumentModel doc) {
        this.doc = doc;
    }

    protected void setMetadataFrom(WebContentData data) throws ClientException {
        doc.setPropertyValue("webcontent:title", data.getTitle());
        doc.setPropertyValue("webcontent:height", data.getHeight());
        doc.setPropertyValue("webcontent:position", data.getPosition());
        doc.setPropertyValue("webcontent:isinaportlet", data.isInAPorlet());
        doc.setPropertyValue("webcontent:iscollapsed", data.isCollapsed());

        List<Map<String, Serializable>> preferences = new ArrayList<Map<String, Serializable>>();

        for (Entry<String, String> entry : data.getPreferences().entrySet()) {
            Map<String, Serializable> preference = new HashMap<String, Serializable>();

            preference.put("name", entry.getKey().toString());
            preference.put("value", entry.getValue());

            preferences.add(preference);
        }

        doc.setPropertyValue("webcontent:preferences",
                (Serializable) preferences);
    }

    @SuppressWarnings("unchecked")
    protected void getMetadataFor(WebContentData data) throws ClientException {
        data.setId(doc.getId());
        data.setName(doc.getName());
        data.setTitle((String) doc.getPropertyValue("webcontent:title"));
        data.setUnitId(doc.getCoreSession().getDocument(doc.getParentRef()).getId());
        data.setHeight((Long) doc.getPropertyValue("webcontent:height"));
        data.setPosition((Long) doc.getPropertyValue("webcontent:position"));
        data.setIsInAPortlet((Boolean) doc.getPropertyValue("webcontent:isinaportlet"));
        data.setIsCollapsed((Boolean) doc.getPropertyValue("webcontent:iscollapsed"));

        data.setOwner((String) doc.getPropertyValue("dc:creator"));
        data.setViewer(doc.getCoreSession().getPrincipal().getName());

        List<Map<String, Serializable>> preferences = (List<Map<String, Serializable>>) doc.getPropertyValue("webcontent:preferences");

        for (Map<String, Serializable> preference : preferences) {
            String name = (String) preference.get("name");
            String value = (String) preference.get("value");
            data.addPreference(
                    DefaultPortletPreference.valueOf(name).toString(), value);
        }
    }
}
