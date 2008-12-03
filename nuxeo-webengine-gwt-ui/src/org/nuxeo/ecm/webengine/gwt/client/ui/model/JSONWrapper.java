package org.nuxeo.ecm.webengine.gwt.client.ui.model;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Window;

public class JSONWrapper implements DocumentConstants{
    JSONObject json;

    public JSONWrapper(JSONObject object) {
        this.json = object;
    }

    protected String getString(String key){
        if ( json != null ){
            JSONValue value = json.get(key);
            if ( value != null && value.isString() != null ){
                return value.isString().stringValue();
            }
        }
        return null;
    }

    protected String[] getStringArray(String key){
        if ( json != null ){
            JSONValue value = json.get(key);
            if ( value != null && value.isArray() != null){
                JSONArray array = value.isArray();
                int size= array.size();
                String[] ret = new String[size];
                for( int i = 0; i < size; i++){
                    JSONValue v = array.get(i);
                    if ( v != null && v.isString()!=null) {
                        ret[i] = v.isString().stringValue();
                    } else {
                        ret[i] = "";
                    }
                }
                return ret;
            }
        }
        return null;
    }

}
