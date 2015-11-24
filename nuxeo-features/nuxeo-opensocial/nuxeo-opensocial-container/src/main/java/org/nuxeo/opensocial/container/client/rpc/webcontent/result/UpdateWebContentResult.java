package org.nuxeo.opensocial.container.client.rpc.webcontent.result;

import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;

import net.customware.gwt.dispatch.shared.Result;

/**
 * @author Stéphane Fourrier
 */
public class UpdateWebContentResult implements Result {
    private static final long serialVersionUID = 1L;

    private WebContentData data;

    @SuppressWarnings("unused")
    private UpdateWebContentResult() {
    }

    public UpdateWebContentResult(WebContentData data) {
        this.data = data;
    }

    public WebContentData getWebContentData() {
        return data;
    }
}
