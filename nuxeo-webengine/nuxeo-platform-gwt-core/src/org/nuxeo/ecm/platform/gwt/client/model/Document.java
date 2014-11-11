package org.nuxeo.ecm.platform.gwt.client.model;

import java.util.Date;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;

public class Document extends JSONWrapper implements DocumentConstants {

    public Document(JSONObject object) {
        super(object);
    }

    public JSONValue getProperty(String schema, String property){
        JSONValue o1 = json.get(schema);
        if ( o1 != null && o1.isObject() != null ){
            JSONValue o2 = o1.isObject().get(property);
            return o2;
        }
        return null;
    }

    public String getStringProperty(String schema, String property ){
        JSONValue v = getProperty(schema, property);
        if ( v != null && v.isString() != null){
            return v.isString().stringValue();
        }
        return null;
    }

    public String getDateProperty(String schema, String property ){
        JSONValue v = getProperty(schema, property);
        if ( v != null && v.isObject() != null){
            JSONObject o = v.isObject();
            JSONValue time = o.get("timeInMillis");
            if ( time != null && time.isNumber()!= null) {
                Date date = new Date((long)time.isNumber().doubleValue());
                return date.toString();
            }
        }
        return null;
    }


    public boolean getBooleanProperty(String schema, String property, boolean defaultValue){
        JSONValue v = getProperty(schema, property);
        if ( v != null && v.isString() != null){
            return v.isBoolean().booleanValue();
        }
        return defaultValue;
    }

    public double getNumberProperty(String schema, String property, double defaultValue ){
        JSONValue v = getProperty(schema, property);
        if ( v != null && v.isString() != null){
            return v.isNumber().doubleValue();
        }
        return defaultValue;
    }

    public String getTitle() {
        return getStringProperty("dublincore", "title");
    }
    
    public String getParentId() {
        return getString(KEY_PARENT_ID);
    }

    public String getDescription() {
        return getStringProperty("dublincore", "description");
    }

    public String getType(){
        return getString(KEY_TYPE);
    }

    public String[] getFacets() {
        return getStringArray(KEY_FACETS);
    }

    public boolean isFolder() {
        String[] facets = getFacets();
        for (String facet : facets) { 
            if (facet.equals("Folderish")) {
                return true;
            }
        }
        return false;
    }
    
    public String[] getSchemas() {
        return getStringArray(KEY_SCHEMAS);
    }

    public String getId() {
        return getString(KEY_ID);
    }

    public String getPath() {
        return getString(KEY_PATH);
    }

}
