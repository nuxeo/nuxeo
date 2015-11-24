package org.nuxeo.opensocial.container.shared.webcontent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.opensocial.container.server.utils.UrlBuilder;
import org.nuxeo.opensocial.container.shared.webcontent.abs.AbstractWebContentData;
import org.nuxeo.opensocial.helper.OpenSocialGadgetHelper;

/**
 * @author Stéphane Fourrier
 */
public class OpenSocialData extends AbstractWebContentData {
    private static final long serialVersionUID = 1L;

    public static final String URL_PREFERENCE = "WC_GADGET_DEF_URL";

    public static final String NAME_PREFERENCE = "WC_GADGET_NAME";

    private static final String ICONE_SUFIX = "-icon";

    private static final String DEFAULT_ICON_NAME = "default-icon";

    public static String TYPE = "wcopensocial";

    private String frameUrl;

    private String gadgetDefUrl;

    private String securityToken;

    private String gadgetName;

    private List<UserPref> userPrefs;

    private Map<String, String> modulePreferences;

    public OpenSocialData() {
        super();
        setUserPrefs(new ArrayList<UserPref>());
        setModulePreferences(new HashMap<String, String>());
    }

    public String getFrameUrl() {
        return frameUrl;
    }

    public void setFrameUrl(String frameUrl) {
        this.frameUrl = frameUrl;
    }

    public String getGadgetDef() {
        return gadgetDefUrl;
    }

    public void setGadgetDef(String gadgetDefUrl) {
        this.gadgetDefUrl = gadgetDefUrl;
    }

    public void setSecurityToken(String securityToken) {
        this.securityToken = securityToken;
    }

    public String getSecurityToken() {
        return securityToken;
    }

    public void setUserPrefs(List<UserPref> userPrefs) {
        this.userPrefs = userPrefs;
    }

    public List<UserPref> getUserPrefs() {
        return userPrefs;
    }

    public UserPref getUserPrefByName(String name) {
        if (name != null) {
            for (UserPref pref : userPrefs) {
                if (name.equals(pref.getName())) {
                    return pref;
                }
            }
        }
        return null;
    }

    public void setModulePreferences(Map<String, String> modulePreferences) {
        this.modulePreferences = modulePreferences;
    }

    public Map<String, String> getModulePreferences() {
        return modulePreferences;
    }

    public void setGadgetName(String name) {
        this.gadgetName = name;
    }

    public String getGadgetName() {
        return gadgetName;
    }

    @Override
    public boolean initPrefs(Map<String, String> params) {
        if (params.get(URL_PREFERENCE) == null
                || params.get(NAME_PREFERENCE) == null) {
            return false;
        } else {
            setGadgetDef(params.get(URL_PREFERENCE));
            setGadgetName(params.get(NAME_PREFERENCE));
        }
        return super.initPrefs(params);
    }

    public void updateFrom(WebContentData data) {
        this.frameUrl = ((OpenSocialData) data).getFrameUrl();
    }

    @Override
    public String getAssociatedType() {
        return TYPE;
    }

    @Override
    public String getIcon() {
        if (getGadgetName() != null) {
            return getGadgetName() + ICONE_SUFIX;
        } else {
            return DEFAULT_ICON_NAME;
        }
    }

    public boolean hasFiles() {
        return false;
    }

}
