package org.nuxeo.webengine.sites;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;

public class SiteHelper {

    public static String getString(DocumentModel d, String xpath, String defaultValue) {
        try {
            return getString(d, xpath);
        } catch (ClientException e) {
            return defaultValue;
        }
    }

    public static String getString(DocumentModel d, String xpath) throws  ClientException{
        Property p = d.getProperty(xpath);
        if ( p != null) {
            Serializable v = p.getValue();
            if ( v != null ) {
                return v.toString();
            }
        }
        return "";
    }

    public static Object getBlob(DocumentModel d, String xpath) throws  ClientException{
        Property p = d.getProperty(xpath);
        if ( p != null) {
            Serializable v = p.getValue();
            if ( v != null ) {
                return v;
            }
        }
        return "";
    }


//    public static String getValue(DocumentModel d, String xpath) throws  ClientException{
//        Property p = d.getProperty(xpath);
//        if ( p != null) {
//            Serializable v = p.getValue();
//            if ( v != null ) {
//                return v.toString();
//            }
//        }
//        return "";
//    }


}
