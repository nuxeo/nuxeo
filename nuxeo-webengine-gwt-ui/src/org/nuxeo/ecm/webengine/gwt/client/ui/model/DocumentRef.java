package org.nuxeo.ecm.webengine.gwt.client.ui.model;

import com.google.gwt.json.client.JSONObject;

public class DocumentRef extends JSONWrapper {


    public DocumentRef(JSONObject object) {
        super(object);
    }

    public boolean isFolderish(){
        if ( json != null ){
            return json.get(KEY_IS_FOLDERISH).isBoolean().booleanValue();
        }
        return false;
    }

    public String getId() {
        return getString(KEY_ID);
    }

    public String getTitle() {
        return getString(KEY_TITLE);
    }

    public String getPath() {
        return getString(KEY_PATH);
    }

    public String getType(){
        return getString(KEY_TYPE);
    }

}
