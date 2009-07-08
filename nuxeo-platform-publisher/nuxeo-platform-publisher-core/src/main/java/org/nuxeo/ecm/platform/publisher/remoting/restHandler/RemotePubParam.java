package org.nuxeo.ecm.platform.publisher.remoting.restHandler;

import java.util.List;

import javax.ws.rs.core.MediaType;

import org.nuxeo.ecm.platform.publisher.api.PublicationNode;

public class RemotePubParam {

    public static final MediaType mediaType = new MediaType("nuxeo",
            "remotepub");

    protected List<Object> params;

    public RemotePubParam(List<Object> params) {
        this.params = params;
    }

    public List<Object> getParams() {
        return params;
    }

    public PublicationNode getAsNode() {
        if (params.size() == 1) {
            return (PublicationNode) params.get(0);
        } else
            return null;
    }

}
