/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.shibboleth.service;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("config")
public class ShibbolethAuthenticationConfig {

    @XNodeMap(value = "uidHeaders/uidHeader", key = "@idpUrl", type = HashMap.class, componentType = String.class)
    protected Map<String, String> uidHeaders = new HashMap<>();

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

    @XNode("@headerEncoding")
    protected String headerEncoding = "UTF-8";

    @XNodeMap(value = "fieldMapping", key = "@header", type = HashMap.class, componentType = String.class)
    protected Map<String, String> fieldMapping = new HashMap<>();

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

    public String getHeaderEncoding() { return headerEncoding; }
}
