package org.nuxeo.ecm.webengine.gwt.client.ui.model;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;

public class Document extends JSONWrapper{

    public Document(JSONObject object) {
        super(object);
    }


    public String getStringProperty(String schema, String property ){
        JSONValue o1 = json.get(schema);
        if ( o1 != null && o1.isObject() != null ){
            JSONValue o2 = o1.isObject().get(property);
            if ( o2 != null && o2.isString() != null ){
                return o2.isString().stringValue();
            }
        }
        return null;
    }

    @Override
    public String getTitle() {
        return getStringProperty("dublincore", "title");
    }

    public String getDescription() {
        return getStringProperty("dublincore", "description");
    }


}
