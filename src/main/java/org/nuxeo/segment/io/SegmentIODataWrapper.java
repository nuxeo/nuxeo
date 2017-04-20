package org.nuxeo.segment.io;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;

import com.github.segmentio.models.Props;

public class SegmentIODataWrapper {

    public static final String LOGIN_KEY = "login";

    public static final String PRINCIPAL_KEY = "principal";

    public static final String GROUP_KEY_PREFIX = "group_";

    protected static final Log log = LogFactory.getLog(SegmentIODataWrapper.class);

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

        // allow override
        if (metadata.containsKey(LOGIN_KEY)) {
            userId = (String) metadata.get(LOGIN_KEY);
        }

        this.metadata = metadata;
    }

    public String getUserId() {
        return userId;
    }

    // code copied from com.github.segmentio.models.Props
    private boolean isPrimitive (Object value) {
        boolean primitive = false;
        if (value != null) {
            Class<?> clazz = value.getClass();
            // http://stackoverflow.com/questions/709961/determining-if-an-object-is-of-primitive-type
            primitive = clazz.isPrimitive() || ClassUtils.wrapperToPrimitive(clazz) != null;
        }
        return primitive;
    }

    // code copied from com.github.segmentio.models.Props
    protected boolean isAllowed(Object value) {
        if (isPrimitive(value) ||
            value instanceof String ||
            value instanceof Date ||
            value instanceof Props ||
            value instanceof BigDecimal ||
            value instanceof Collection ||
            value instanceof Map ||
            value instanceof Object[]) {
            return true;
        } else {
            return false;
        }
    }

    public Map<String, Serializable> getMetadata() {
        Map<String, Serializable> map = new HashMap<>();
        for (String key : metadata.keySet()) {
            if (!key.startsWith(GROUP_KEY_PREFIX)) {
                Serializable value = metadata.get(key);
                if (value!=null) {
                    if ( isAllowed(value)) {
                        map.put(key, value);
                    } else {
                        map.put(key, value.toString());
                    }
                } else {
                    log.debug("Skip null value for key " + key);
                }
            }
        }
        return map;
    }

    public Map<String, Serializable> getGroupMetadata() {
        Map<String, Serializable> map = new HashMap<>();
        for (String key : metadata.keySet()) {
            if (key.startsWith(GROUP_KEY_PREFIX)) {
                String gKey = key.substring(GROUP_KEY_PREFIX.length());
                Serializable value = metadata.get(key);
                if (value!=null) {
                    if ( isAllowed(value)) {
                        map.put(gKey, value);
                    } else {
                        map.put(gKey, value.toString());
                    }
                } else {
                    log.debug("Skip null value for key " + key);
                }
            }
        }
        return map;
    }


}
