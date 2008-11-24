package org.nuxeo.ecm.webengine.gwt.client.ui.model;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;

public class DocumentRef {
    JSONObject object;

    public DocumentRef(JSONObject object) {
        this.object = object;
    }

    private static final String KEY_ID = "id";
    private static final String KEY_IS_FOLDERISH = "isFolderish";
    private static final String KEY_TITLE = "title";
    private static final String KEY_PATH = "path";


    public String getId(){
        if ( object != null ){
            JSONValue value = object.get(KEY_ID);
            if ( value != null ){
                return value.isString().stringValue();
            }
        }
        return null;
    }



    public String getTitle(){
        if ( object != null ){
            JSONValue value = object.get(KEY_TITLE);
            if ( value != null ){
                return value.isString().stringValue();
            }
        }
        return null;
    }



    public String getPath(){
        if ( object != null ){
            JSONValue value = object.get(KEY_PATH);
            if ( value != null ){
                return value.isString().stringValue();
            }
        }
        return null;
    }

    public boolean isFolderish(){
        if ( object != null ){
            return object.get(KEY_IS_FOLDERISH).isBoolean().booleanValue();
        }
        return false;
    }

}
