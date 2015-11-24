package org.nuxeo.opensocial.container.server.webcontent.abs;

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
import org.nuxeo.opensocial.container.server.webcontent.api.WebContentAdapter;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;

/**
 * @author Stéphane Fourrier
 */
public abstract class AbstractWebContentAdapter<T extends WebContentData>
        implements WebContentAdapter<T> {

    protected DocumentModel doc;

    public AbstractWebContentAdapter(DocumentModel doc) {
        this.doc = doc;
    }

    public void setPosition(long position) throws ClientException {
        doc.setPropertyValue(WEB_CONTENT_POSITION_PROPERTY, position);
    }

    public long getPosition() throws ClientException {
        return (Long) doc.getPropertyValue(WEB_CONTENT_POSITION_PROPERTY);
    }

    public String getTitle() throws ClientException {
        return (String) doc.getPropertyValue(WEB_CONTENT_TITLE_PROPERTY);
    }

    public void setTitle(String title) throws ClientException {
        doc.setPropertyValue(WEB_CONTENT_TITLE_PROPERTY, title);
    }

    public boolean isInAPortlet() throws ClientException {
        return (Boolean) doc.getPropertyValue(WEB_CONTENT_IS_IN_A_PORTLET_PROPERTY);
    }

    public void setInAPortlet(boolean isInAPortlet) throws ClientException {
        doc.setPropertyValue(WEB_CONTENT_IS_IN_A_PORTLET_PROPERTY, isInAPortlet);
    }

    public boolean isCollapsed() throws ClientException {
        return (Boolean) doc.getPropertyValue(WEB_CONTENT_IS_COLLAPSED_PROPERTY);
    }

    public void setCollapsed(boolean isCollapsed) throws ClientException {
        doc.setPropertyValue(WEB_CONTENT_IS_COLLAPSED_PROPERTY, isCollapsed);
    }

    protected void setMetadataFrom(WebContentData data) throws ClientException {
        setTitle(data.getTitle());
        setPosition(data.getPosition());
        setInAPortlet(data.isInAPorlet());
        setCollapsed(data.isCollapsed());

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
        data.setTitle(getTitle());
        data.setUnitId(doc.getCoreSession().getDocument(doc.getParentRef()).getId());
        data.setPosition(getPosition());
        data.setIsInAPortlet(isInAPortlet());
        data.setIsCollapsed(isCollapsed());

        data.setOwner((String) doc.getPropertyValue("dc:creator"));
        data.setViewer(doc.getCoreSession().getPrincipal().getName());

        List<Map<String, Serializable>> preferences = (List<Map<String, Serializable>>) doc.getPropertyValue(WEB_CONTENT_PREFERENCES_PROPERTY);
        for (Map<String, Serializable> preference : preferences) {
            String name = (String) preference.get("name");
            String value = (String) preference.get("value");
            data.addPreference(name, value);
        }
    }

    @Override
    public void update() throws ClientException {
        doc.getCoreSession().saveDocument(doc);
    }
}
