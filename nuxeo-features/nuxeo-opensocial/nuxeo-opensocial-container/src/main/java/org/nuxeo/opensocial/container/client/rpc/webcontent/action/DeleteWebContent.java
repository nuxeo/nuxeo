package org.nuxeo.opensocial.container.client.rpc.webcontent.action;

import org.nuxeo.opensocial.container.client.rpc.AbstractAction;
import org.nuxeo.opensocial.container.client.rpc.ContainerContext;
import org.nuxeo.opensocial.container.client.rpc.webcontent.result.DeleteWebContentResult;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;

/**
 * @author Stéphane Fourrier
 */
public class DeleteWebContent extends AbstractAction<DeleteWebContentResult> {

    private static final long serialVersionUID = 1L;

    private WebContentData data;

    @SuppressWarnings("unused")
    @Deprecated
    // For serialization purpose only
    private DeleteWebContent() {
        super();
    }

    public DeleteWebContent(ContainerContext containerContext,
            final WebContentData webContentData) {
        super(containerContext);
        this.data = webContentData;
    }

    public WebContentData getData() {
        return data;
    }

}
