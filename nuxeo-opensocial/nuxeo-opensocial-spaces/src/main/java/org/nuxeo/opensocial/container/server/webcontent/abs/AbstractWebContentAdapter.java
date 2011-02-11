package org.nuxeo.opensocial.container.server.webcontent.abs;

import static org.nuxeo.ecm.spaces.api.Constants.WEB_CONTENT_HEIGHT_PROPERTY;
import static org.nuxeo.ecm.spaces.api.Constants.WEB_CONTENT_IS_COLLAPSED_PROPERTY;
import static org.nuxeo.ecm.spaces.api.Constants.WEB_CONTENT_IS_IN_A_PORTLET_PROPERTY;
import static org.nuxeo.ecm.spaces.api.Constants.WEB_CONTENT_POSITION_PROPERTY;
import static org.nuxeo.ecm.spaces.api.Constants.WEB_CONTENT_PREFERENCES_PROPERTY;
import static org.nuxeo.ecm.spaces.api.Constants.WEB_CONTENT_TITLE_PROPERTY;

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
        doc.setPropertyValue(WEB_CONTENT_TITLE_PROPERTY, data.getTitle());
        doc.setPropertyValue(WEB_CONTENT_HEIGHT_PROPERTY, data.getHeight());
        doc.setPropertyValue(WEB_CONTENT_POSITION_PROPERTY, data.getPosition());
        doc.setPropertyValue(WEB_CONTENT_IS_IN_A_PORTLET_PROPERTY,
                data.isInAPorlet());
        doc.setPropertyValue(WEB_CONTENT_IS_COLLAPSED_PROPERTY,
                data.isCollapsed());

        List<Map<String, Serializable>> preferences = new ArrayList<Map<String, Serializable>>();

        for (Entry<String, String> entry : data.getPreferences().entrySet()) {
            Map<String, Serializable> preference = new HashMap<String, Serializable>();

            preference.put("name", entry.getKey().toString());
            preference.put("value", entry.getValue());

            preferences.add(preference);
        }

        doc.setPropertyValue(WEB_CONTENT_PREFERENCES_PROPERTY,
                (Serializable) preferences);
    }

    @SuppressWarnings("unchecked")
    protected void getMetadataFor(WebContentData data) throws ClientException {
        data.setId(doc.getId());
        data.setName(doc.getName());
        data.setTitle((String) doc.getPropertyValue(WEB_CONTENT_TITLE_PROPERTY));
        data.setUnitId(doc.getCoreSession().getDocument(doc.getParentRef()).getId());
        data.setHeight((Long) doc.getPropertyValue(WEB_CONTENT_HEIGHT_PROPERTY));
        data.setPosition((Long) doc.getPropertyValue(WEB_CONTENT_POSITION_PROPERTY));
        data.setIsInAPortlet((Boolean) doc.getPropertyValue(WEB_CONTENT_IS_IN_A_PORTLET_PROPERTY));
        data.setIsCollapsed((Boolean) doc.getPropertyValue(WEB_CONTENT_IS_COLLAPSED_PROPERTY));

        data.setOwner((String) doc.getPropertyValue("dc:creator"));
        data.setViewer(doc.getCoreSession().getPrincipal().getName());

        List<Map<String, Serializable>> preferences = (List<Map<String, Serializable>>) doc.getPropertyValue(WEB_CONTENT_PREFERENCES_PROPERTY);

        for (Map<String, Serializable> preference : preferences) {
            String name = (String) preference.get("name");
            String value = (String) preference.get("value");
            data.addPreference(
                    DefaultPortletPreference.valueOf(name).toString(), value);
        }
    }
}
