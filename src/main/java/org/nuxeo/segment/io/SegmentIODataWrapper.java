package org.nuxeo.segment.io;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;

public class SegmentIODataWrapper {

    public static final String LOGIN_KEY = "login";

    public static final String PRINCIPAL_KEY = "principal";

    protected String userId;
    protected Map<String, Serializable> metadata;

    public SegmentIODataWrapper(NuxeoPrincipal principal, Map<String, Serializable> metadata) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }

        if (metadata.containsKey(PRINCIPAL_KEY) && metadata.get(PRINCIPAL_KEY)!=null) {
            principal = (NuxeoPrincipal) metadata.get(PRINCIPAL_KEY);
        }

        userId = principal.getName();
        if (!metadata.containsKey("email")) {
            metadata.put("email", principal.getEmail());
        }
        if (!metadata.containsKey("firstName")) {
            metadata.put("firstName", principal.getFirstName());
        }
        if (!metadata.containsKey("lastName")) {
            metadata.put("lastName", principal.getLastName());
        }

        // allow override
        if (metadata.containsKey(LOGIN_KEY)) {
            userId = (String) metadata.get(LOGIN_KEY);
        }

        this.metadata = metadata;
    }

    public String getUserId() {
        return userId;
    }

    public Map<String, Serializable> getMetadata() {
        return metadata;
    }

}
