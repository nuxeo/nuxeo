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

}
