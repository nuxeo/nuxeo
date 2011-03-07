package org.nuxeo.ecm.platform.wi.filter;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Organization: Gagnavarslan ehf
 */
public class WISession implements Serializable {

    public static final String BACKEND_KEY = "org.nuxeo.ecm.platform.wi.backend";
    public static final String CORESESSION_KEY = "org.nuxeo.ecm.platform.wi.coresession";

    private Map<String, Object> attributes = new HashMap<String, Object>();

    private String key;

    private long creationTime;

    private long accessTime;

    public WISession(String key) {
        this.creationTime = System.currentTimeMillis();
        access();
        this.key = key;
    }

    public void setAttribute(String key, Object value){
        this.attributes.put(key, value);
    }

    public Object getAttribute(String key){
        return this.attributes.get(key);
    }

    public Collection getAttributes() {
        return attributes.values();
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void access(){
        this.accessTime = System.currentTimeMillis();
    }

    public boolean isValid(){
        long time = System.currentTimeMillis();
        if(time > creationTime + 20 * 60 * 1000 && time > accessTime + 2*60 * 1000){
            return false;
        } else {
            return true;
        }
    }
}
