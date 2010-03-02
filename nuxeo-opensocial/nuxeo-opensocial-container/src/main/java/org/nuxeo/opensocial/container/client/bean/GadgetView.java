package org.nuxeo.opensocial.container.client.bean;

import com.google.gwt.user.client.rpc.IsSerializable;

public class GadgetView implements IsSerializable {

    private String view;

    private String contentType;

    public GadgetView() {
    }

    public GadgetView(String view, String contentType) {
        this.view = view;
        this.contentType = contentType;
    }

    public String getView() {
        return view;
    }

    public String getContentType() {
        return contentType;
    }

}
