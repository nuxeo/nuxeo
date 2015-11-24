package org.nuxeo.ecm.platform.shibboleth.service;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("config")
public class ShibbolethAuthenticationConfig {

    @XNodeMap(value = "uidHeaders/uidHeader", key = "@idpUrl", type = HashMap.class, componentType = String.class)
    protected Map<String, String> uidHeaders = new HashMap<String, String>();

    @XNode("uidHeaders/default")
    protected String defaultUidHeader;

    @XNode("loginURL")
    protected String loginURL;

    @XNode("loginRedirectURLParameter")
    protected String loginRedirectURLParameter = "target";

    @XNode("logoutURL")
    protected String logoutURL;

    @XNode("logoutRedirectURLParameter")
    protected String logoutRedirectURLParameter = "return";

    @XNode("idpHeader")
    protected String idpHeader = "shib-identity-provider";

    @XNodeMap(value = "fieldMapping", key = "@header", type = HashMap.class, componentType = String.class)
    protected Map<String, String> fieldMapping = new HashMap<String, String>();

    public Map<String, String> getUidHeaders() {
        return uidHeaders;
    }

    public String getDefaultUidHeader() {
        return defaultUidHeader;
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

    public String getIdpHeader() {
        return idpHeader;
    }

}
