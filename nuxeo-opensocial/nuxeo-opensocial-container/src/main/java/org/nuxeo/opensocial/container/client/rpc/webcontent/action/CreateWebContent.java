package org.nuxeo.opensocial.container.client.rpc.webcontent.action;

import org.nuxeo.opensocial.container.client.rpc.AbstractAction;
import org.nuxeo.opensocial.container.client.rpc.ContainerContext;
import org.nuxeo.opensocial.container.client.rpc.webcontent.result.CreateWebContentResult;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;

/**
 * @author Stéphane Fourrier
 */
public class CreateWebContent extends AbstractAction<CreateWebContentResult> {
    private static final long serialVersionUID = 1L;

    private WebContentData data;

    @SuppressWarnings("unused")
    @Deprecated
    // For serialization purpose only
    private CreateWebContent() {
        super();
    }

    public CreateWebContent(ContainerContext containerContext,
            final WebContentData data) {
        super(containerContext);
        this.data = data;
    }

    public WebContentData getData() {
        return data;
    }

}
