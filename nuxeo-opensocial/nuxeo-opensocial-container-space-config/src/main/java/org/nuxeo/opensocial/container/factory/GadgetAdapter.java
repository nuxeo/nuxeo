package org.nuxeo.opensocial.container.factory;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.spaces.api.Gadget;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;
import org.nuxeo.opensocial.container.client.bean.PreferencesBean;

/**
 * @author Guillaume Cusnieux
 */
class GadgetAdapter implements Gadget {

    private int heigth;

    private String htmlContent;

    private String viewer;

    private String title;

    private Map<String, String> preferences;

    private String placeId;

    private int position;

    private String name;

    private String id;

    private boolean collapsed;

    public GadgetAdapter(GadgetBean bean) throws ClientException {
        this.setHeight(bean.getHeight());
        this.setHtmlContent(bean.getHtmlContent());
        this.setViewer(bean.getViewer());
        this.setTitle(bean.getTitle());
        this.setPreferences(createPreferences(bean));
        this.setPlaceId(bean.getPosition().getPlaceID());
        this.setPosition(bean.getPosition().getPosition());
        this.setName(bean.getName());
        this.setId(bean.getRef());
        this.setCollapsed(bean.isCollapsed());
    }

    private Map<String, String> createPreferences(GadgetBean bean) {
        Map<String, String> prefs = new HashMap<String, String>();
        return buildPreferences(buildPreferences(prefs, bean.getUserPrefs()),
                bean.getDefaultPrefs());

    }

    private Map<String, String> buildPreferences(Map<String, String> prefs,
            List<PreferencesBean> uPrefs) {
        if (uPrefs != null) {
            for (PreferencesBean p : uPrefs) {
                prefs.put(p.getName(), p.getValue());
            }
        }
        return prefs;
    }

    public int getHeigth() {
        return heigth;
    }

    public void setHeigth(int heigth) {
        this.heigth = heigth;
    }

    public void setViewer(String viewer) {
        this.viewer = viewer;
    }

    public void setOwner(String owner) {
    }

    public void setId(String id) {
        this.id = id;
    }

    public void copyFrom(Gadget gadget) throws ClientException {
    }

    public String getCategory() throws ClientException {
        return null;
    }

    public URL getDefinitionUrl() throws ClientException {
        return null;
    }

    public String getDescription() throws ClientException {
        return null;
    }

    public int getHeight() throws ClientException {
        return heigth;
    }

    public String getHtmlContent() throws ClientException {
        return htmlContent;
    }

    public String getId() {
        return id;
    }

    public String getName() throws ClientException {
        return name;
    }

    public String getOwner() throws ClientException {
        return null;
    }

    public Space getParent() throws ClientException {
        return null;
    }

    public int getPosition() throws ClientException {
        return position;
    }

    public String getPref(String prefKey) throws ClientException {
        return preferences.get(prefKey);
    }

    public Map<String, String> getPreferences() throws ClientException {
        return preferences;
    }

    public String getTitle() throws ClientException {
        return title;
    }

    public String getViewer() throws ClientException {
        return viewer;
    }

    public boolean hasPermission(String permissioName) {
        return false;
    }

    public boolean isCollapsed() throws ClientException {
        return collapsed;
    }

    public boolean isEqualTo(Gadget gadget) throws ClientException {
        return false;
    }

    public void setCategory(String category) throws ClientException {
    }

    public void setCollapsed(boolean collapsed) throws ClientException {
        this.collapsed = collapsed;
    }

    public void setDefinitionUrl(URL url) throws ClientException {
    }

    public void setDescription(String description) throws ClientException {
    }

    public void setHeight(int height) throws ClientException {
        this.heigth = height;
    }

    public void setHtmlContent(String htmlContent) throws ClientException {
        this.htmlContent = htmlContent;
    }

    public void setName(String name) throws ClientException {
        this.name = name;
    }

    public void setPosition(int position) throws ClientException {
        this.position = position;
    }

    public void setPreferences(Map<String, String> prefs)
            throws ClientException {
        this.preferences = prefs;
    }

    public void setTitle(String title) throws ClientException {
        this.title = title;
    }

    public String getPlaceId() throws ClientException {
        return placeId;
    }

    public void setPlaceId(String placeId) throws ClientException {
        this.placeId = placeId;
    }

    public void save() throws ClientException {

    }

}
