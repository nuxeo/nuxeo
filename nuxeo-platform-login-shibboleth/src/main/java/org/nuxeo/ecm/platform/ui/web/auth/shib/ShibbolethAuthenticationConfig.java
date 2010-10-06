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

    @XNodeMap(value = "fieldMapping", key = "@header", type = HashMap.class, componentType = String.class)
    protected Map<String, String> fieldMapping = new HashMap<String, String>();

    public String getUidHeader() {
        return uidHeader;
    }

    public Map<String, String> getFieldMapping() {
        return fieldMapping;
    }

}
