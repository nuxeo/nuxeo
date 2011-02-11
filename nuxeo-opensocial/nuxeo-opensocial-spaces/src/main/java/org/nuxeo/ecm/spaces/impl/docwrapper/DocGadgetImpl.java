package org.nuxeo.ecm.spaces.impl.docwrapper;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.spaces.api.AbstractGadget;
import org.nuxeo.ecm.spaces.api.Gadget;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.opensocial.gadgets.service.api.GadgetService;
import org.nuxeo.runtime.api.Framework;

public class DocGadgetImpl extends AbstractGadget {

    private static final String GADGET_POSITION = "webcontent:position";

    private static final String GADGET_COLLAPSED = "webcontent:iscollapsed";

    private static final String GADGET_HEIGHT = "webcontent:height";

    private static final String GADGET_PREFERENCES = "webcontent:preferences";

    private static final String GADGET_NAME = "wcopensocial:gadgetname";

    private static final String GADGET_URL = "wcopensocial:gadgetDefUrl";

    public static final String TYPE = "WCOpenSocial";

    private final DocumentModel doc;

    private static final Log LOGGER = LogFactory.getLog(DocGadgetImpl.class);

    public DocGadgetImpl(DocumentModel doc) {
        this.doc = doc;
    }

    public DocumentModel getDocument() {
        return doc;
    }

    public String getCategory() throws ClientException {
        throw new ClientException("Deprecated");
    }

    public URL getDefinitionUrl() throws ClientException {
        GadgetService service;
        URL url = null;
        try {
            service = Framework.getService(GadgetService.class);
            url = service.getGadgetDefinition(this.getName());
        } catch (Exception e) {
            LOGGER.warn("Unable to get URL from gadgetService", e);
        }

        if (url == null) {
            try {
                url = new URL((String) doc.getPropertyValue(GADGET_URL));
            } catch (MalformedURLException e) {
                LOGGER.error("Malformed URL for gadget " + getId(), e);
                return null;
            }
        }
        return url;
    }

    public String getDescription() throws ClientException {
        return (String) doc.getPropertyValue("dc:description");
    }

    public String getId() {
        return doc.getId();
    }

    public String getName() throws ClientException {
        return (String) doc.getPropertyValue(GADGET_NAME);
    }

    public String getOwner() throws ClientException {
        String owner = (String) doc.getPropertyValue("dc:creator");
        // this CANNOT be null because OAuth will reject any attempt to have the
        // owner of a gadget be null

        // normally, in opensocial.properties you must
        // shindig.signing.viewer-access-tokens-enabled=true
        // but this is unsafe in the case where you can have people
        // that can see other users dashboards
        return owner == null ? "unknown" : owner;
    }

    public Space getParent() throws ClientException {
        CoreSession session = doc.getCoreSession();
        DocumentModel parent = session.getDocument(doc.getParentRef());
        return parent.getAdapter(Space.class);
    }

    public String getPlaceId() throws ClientException {
        throw new ClientException("Deprecated");
    }

    public int getPosition() throws ClientException {
        Long result = (Long) doc.getPropertyValue(GADGET_POSITION);
        return (result == null) ? 0 : result.intValue();
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getPreferences() throws ClientException {
        ArrayList<Map<String, String>> list = (ArrayList<Map<String, String>>) doc.getPropertyValue(GADGET_PREFERENCES);
        if (list == null)
            return null;
        HashMap ret = new HashMap<String, String>();
        for (Map<String, String> map : list) {
            ret.put(map.get("name"), map.get("value"));
        }
        return ret;

    }

    public String getTitle() throws ClientException {
        return doc.getTitle();
    }

    public boolean isCollapsed() throws ClientException {
        Boolean result = (Boolean) doc.getPropertyValue(GADGET_COLLAPSED);
        return (result == null) ? false : result.booleanValue();
    }

    public boolean isEqualTo(Gadget gadget) {
        return gadget.getId().equals(getId());
    }

    public void setCategory(String category) throws ClientException {
        throw new ClientException("Deprecated");

    }

    public void setCollapsed(boolean collapsed) throws ClientException {
        doc.setPropertyValue(GADGET_COLLAPSED, collapsed);
    }

    public void setDefinitionUrl(URL url) throws ClientException {
        doc.setPropertyValue(GADGET_URL, url.toString());

    }

    public void setDescription(String description) throws ClientException {
        doc.setPropertyValue("dc:desctription", description);

    }

    public void setName(String name) throws ClientException {
        doc.setPropertyValue(GADGET_NAME, name);

    }

    public void setPlaceId(String placeId) throws ClientException {
        throw new ClientException("Deprecated");

    }

    public void setPosition(int position) throws ClientException {
        doc.setPropertyValue(GADGET_POSITION, position);

    }

    public void setPreferences(Map<String, String> prefs)
            throws ClientException {
        ArrayList<Map<String, String>> listPrefs = new ArrayList<Map<String, String>>();
        for (String key : prefs.keySet()) {
            Map<String, String> keyValue = new HashMap<String, String>();
            keyValue.put("name", key);
            keyValue.put("value", prefs.get(key));
            listPrefs.add(keyValue);
        }
        doc.setPropertyValue(GADGET_PREFERENCES, listPrefs);
    }

    public void setTitle(String title) throws ClientException {
        doc.setPropertyValue("dc:title", title);

    }

    public int getHeight() throws ClientException {
        Long result = (Long) doc.getPropertyValue(GADGET_HEIGHT);
        return (result == null) ? 0 : result.intValue();
    }

    public void setHeight(int height) throws ClientException {
        doc.setPropertyValue(GADGET_HEIGHT, height);
    }

    public void copyFrom(Gadget gadget) throws ClientException {
        this.setTitle(gadget.getTitle());
        this.setPosition(gadget.getPosition());
        this.setHeight(gadget.getHeight());
        this.setCollapsed(gadget.isCollapsed());
        this.setPreferences(gadget.getPreferences());
    }

    public String getViewer() throws ClientException {
        return doc.getCoreSession().getPrincipal().getName();
    }

    public boolean hasPermission(String permissioName) throws ClientException {
        return doc.getCoreSession().hasPermission(doc.getRef(), permissioName);
    }

    public void save() throws ClientException {
        CoreSession session = doc.getCoreSession();
        session.saveDocument(doc);
        session.save();
    }

}
