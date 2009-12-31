package org.nuxeo.ecm.spaces.core.impl.docwrapper;

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
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.spaces.api.AbstractGadget;
import org.nuxeo.ecm.spaces.api.Gadget;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.opensocial.gadgets.service.api.GadgetService;
import org.nuxeo.runtime.api.Framework;

public class DocGadgetImpl extends AbstractGadget {

    private static final String GADGET_CATEGORY = "gadget:category";

    private static final String GADGET_PLACEID = "gadget:placeID";// html
                                                                    // division
                                                                    // id
    private static final String GADGET_POSITION = "gadget:position";// position
                                                                    // in the
                                                                    // div
    private static final String GADGET_COLLAPSED = "gadget:collapsed";// is the
                                                                        // gadget
                                                                        // collapsed

    private static final String GADGET_HEIGHT = "gadget:height";
    private static final String GADGET_PREFERENCES = "gadget:props";
    private static final String GADGET_NAME = "gadget:name";
    private static final String GADGET_URL = "gadget:url";
    public static final String TYPE = "Gadget";

    private final DocumentModel doc;
    private static final Log LOGGER = LogFactory.getLog(DocGadgetImpl.class);

    public DocGadgetImpl(DocumentModel doc) {
        this.doc = doc;

    }

    public DocumentModel getDocument() {
        return doc;
    }

    public String getCategory() throws ClientException {
        return (String) doc.getPropertyValue(GADGET_CATEGORY);
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
        return (String) doc.getPropertyValue("dc:creator");
    }

    public Space getParent() throws ClientException {
        CoreSession session = doc.getCoreSession();
        DocumentModel parent = session.getDocument(doc.getParentRef());
        return parent.getAdapter(Space.class);
    }

    public String getPlaceID() throws ClientException {
        String result = (String) doc.getPropertyValue(GADGET_PLACEID);
        if(result == null) {
            return "";
        } else {
            return result;
        }
    }

    public int getPosition() throws ClientException {
        Integer result = (Integer) doc.getPropertyValue(GADGET_POSITION);
        if(result == null) {
            return 0;
        } else {
            return result;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getPreferences() throws ClientException {
        ArrayList<Map<String, String>> list = (ArrayList<Map<String, String>>) doc
                .getPropertyValue(GADGET_PREFERENCES);

        if (list == null)
            return null;

        HashMap ret = new HashMap<String, String>();
        for (Map<String, String> map : list) {
            String key = map.get("name");
            String value = map.get("value");
            ret.put(key, value);
        }
        return ret;

    }

    public String getTitle() throws ClientException {
        return doc.getTitle();
    }

    public boolean isCollapsed() throws ClientException {
        Boolean result =  (Boolean) doc.getPropertyValue(GADGET_COLLAPSED);
        if (result == null) {
            return false;
        } else {
            return result;
        }
    }

    public boolean isEqualTo(Gadget gadget) {
        return gadget.getId().equals(getId());
    }

    public void setCategory(String category) throws ClientException {
        doc.setPropertyValue(GADGET_CATEGORY, category);

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
        doc.setPropertyValue(GADGET_PLACEID, placeId);

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
        Integer result = (Integer) doc.getPropertyValue(GADGET_HEIGHT);
        if(result == null) {
            return 0;

        } else {
            return result;
        }
    }

    public void setHeight(int height) throws ClientException {
        doc.setPropertyValue(GADGET_HEIGHT, height);

    }

}
