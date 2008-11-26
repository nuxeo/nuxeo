package org.nuxeo.ecm.webengine.gwt.client.ui.model;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;

public class JSONWrapper implements DocumentConstants{
    JSONObject json;

    public JSONWrapper(JSONObject object) {
        this.json = object;
    }

    public String getId(){
        if ( json != null ){
            JSONValue value = json.get(KEY_ID);
            if ( value != null ){
                return value.isString().stringValue();
            }
        }
        return null;
    }

    public String getTitle(){
        if ( json != null ){
            JSONValue value = json.get(KEY_TITLE);
            if ( value != null ){
                return value.isString().stringValue();
            }
        }
        return null;
    }



    public String getPath(){
        if ( json != null ){
            JSONValue value = json.get(KEY_PATH);
            if ( value != null ){
                return value.isString().stringValue();
            }
        }
        return null;
    }

}
