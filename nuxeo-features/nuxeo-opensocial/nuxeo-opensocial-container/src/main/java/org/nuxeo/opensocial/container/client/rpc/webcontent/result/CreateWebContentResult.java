package org.nuxeo.opensocial.container.client.rpc.webcontent.result;

import java.util.Map;

import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;

import net.customware.gwt.dispatch.shared.Result;

/**
 * @author Stéphane Fourrier
 */
public class CreateWebContentResult implements Result {
    private static final long serialVersionUID = 1L;

    private WebContentData data;

    private Map<String, Boolean> permissions;

    @SuppressWarnings("unused")
    @Deprecated
    // For serialisation purpose only
    private CreateWebContentResult() {
    }

    public CreateWebContentResult(WebContentData data,
            Map<String, Boolean> permissions) {
        this.data = data;
        this.permissions = permissions;
    }

    public WebContentData getData() {
        return data;
    }

    public Map<String, Boolean> getPermissions() {
        return permissions;
    }
}
