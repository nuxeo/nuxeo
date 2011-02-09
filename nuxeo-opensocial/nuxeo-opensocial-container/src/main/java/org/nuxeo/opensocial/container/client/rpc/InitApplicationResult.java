package org.nuxeo.opensocial.container.client.rpc;

import java.util.List;
import java.util.Map;

import org.nuxeo.opensocial.container.shared.layout.api.YUILayout;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;

import net.customware.gwt.dispatch.shared.Result;

/**
 * @author Stéphane Fourrier
 */
public class InitApplicationResult implements Result {
    private static final long serialVersionUID = 1L;

    private YUILayout layout;

    private Map<String, List<WebContentData>> webContents;

    private Map<String, Map<String, Boolean>> permissions;

    private String spaceId;

    public InitApplicationResult(final YUILayout layout,
            final Map<String, List<WebContentData>> webContents,
            final Map<String, Map<String, Boolean>> permissions, String spaceId) {
        this.layout = layout;
        this.webContents = webContents;
        this.permissions = permissions;
        this.spaceId = spaceId;
    }

    @SuppressWarnings("unused")
    private InitApplicationResult() {
    }

    public YUILayout getLayout() {
        return layout;
    }

    public Map<String, List<WebContentData>> getWebContents() {
        return webContents;
    }

    public Map<String, Map<String, Boolean>> getPermissions() {
        return permissions;
    }

    public String getSpaceId() {
        return spaceId;
    }

}
