package org.nuxeo.webengine.sites;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.Blob;
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

    public static String getString(DocumentModel d, String xpath) throws ClientException {
        Property p = d.getProperty(xpath);
        if (p != null) {
            Serializable v = p.getValue();
            if (v != null) {
                return v.toString();
            }
        }
        return "";
    }

    public static Blob getBlob(DocumentModel d, String xpath) throws ClientException {
        Property p = d.getProperty(xpath);
        if (p != null) {
            Serializable v = p.getValue();
            if (v != null) {
                return (Blob) v;
            }
        }
        return null;
    }

    public static boolean getBoolean(DocumentModel d, String xpath, boolean defaultValue) {
        try {
            return getBoolean(d, xpath);
        } catch (ClientException e) {
            return defaultValue;
        }
    }

    public static boolean getBoolean(DocumentModel d, String xpath) throws ClientException {
        Property p = d.getProperty(xpath);
        if (p != null) {
            Serializable v = p.getValue();
            if (v != null) {
                return (Boolean) v;
            }
        }
        throw new ClientException("value is null");
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
