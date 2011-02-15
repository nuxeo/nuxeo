package org.nuxeo.opensocial.container.client.model;

import com.google.gwt.core.client.JavaScriptObject;

public class NXIDPreference extends JavaScriptObject {
    protected NXIDPreference() {
    }

    final public native String getNXId() /*-{
                                         return this.NXID;
                                         }-*/;

    final public native String getNXName() /*-{
                                           return this.NXNAME;
                                           }-*/;
}
