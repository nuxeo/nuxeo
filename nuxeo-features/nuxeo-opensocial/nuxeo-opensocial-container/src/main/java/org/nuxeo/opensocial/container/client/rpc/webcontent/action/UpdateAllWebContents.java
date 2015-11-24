package org.nuxeo.opensocial.container.client.rpc.webcontent.action;

import java.util.List;
import java.util.Map;

import org.nuxeo.opensocial.container.client.rpc.AbstractAction;
import org.nuxeo.opensocial.container.client.rpc.ContainerContext;
import org.nuxeo.opensocial.container.client.rpc.webcontent.result.UpdateAllWebContentsResult;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;

/**
 * @author Stéphane Fourrier
 */
public class UpdateAllWebContents extends
        AbstractAction<UpdateAllWebContentsResult> {

    private static final long serialVersionUID = 1L;

    private Map<String, List<WebContentData>> webContents;

    @SuppressWarnings("unused")
    @Deprecated
    // For serialisation purpose only
    private UpdateAllWebContents() {
        super();
    }

    public UpdateAllWebContents(ContainerContext containerContext,
            final Map<String, List<WebContentData>> map) {
        super(containerContext);
        this.webContents = map;
    }

    public Map<String, List<WebContentData>> getWebContents() {
        return webContents;
    }

}
