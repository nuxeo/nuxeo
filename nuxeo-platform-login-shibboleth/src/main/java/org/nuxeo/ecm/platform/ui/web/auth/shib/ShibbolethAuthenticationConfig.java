package org.nuxeo.ecm.platform.ui.web.auth.shib;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("config")
public class ShibbolethAuthenticationConfig {

    @XNode("uidHeader")
    protected String uidHeader;

    @XNode("loginURL")
    protected String loginURL;

    @XNode("loginRedirectURLParameter")
    protected String loginRedirectURLParameter = "target";

    @XNode("logoutURL")
    protected String logoutURL;

    @XNode("logoutRedirectURLParameter")
    protected String logoutRedirectURLParameter = "return";


    @XNodeMap(value = "fieldMapping", key = "@header", type = HashMap.class, componentType = String.class)
    protected Map<String, String> fieldMapping = new HashMap<String, String>();

    public String getUidHeader() {
        return uidHeader;
    }

    public String getLoginURL() {
        return loginURL;
    }

    public String getLogoutURL() {
        return logoutURL;
    }

    public String getLoginRedirectURLParameter() {
        return loginRedirectURLParameter;
    }

    public String getLogoutRedirectURLParameter() {
        return logoutRedirectURLParameter;
    }

    public Map<String, String> getFieldMapping() {
        return fieldMapping;
    }

}
