package org.nuxeo.opensocial.container.client.rpc.webcontent.action;

import org.nuxeo.opensocial.container.client.rpc.AbstractAction;
import org.nuxeo.opensocial.container.client.rpc.ContainerContext;
import org.nuxeo.opensocial.container.client.rpc.webcontent.result.UpdateWebContentResult;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;

/**
 * @author Stéphane Fourrier
 */
public class UpdateWebContent extends AbstractAction<UpdateWebContentResult> {

    private static final long serialVersionUID = 1L;

    private WebContentData webContent;

    @SuppressWarnings("unused")
    @Deprecated
    // For serialisation purpose only
    private UpdateWebContent() {
        super();
    }

    public UpdateWebContent(ContainerContext containerContext,
            final WebContentData webContentData) {
        super(containerContext);
        this.webContent = webContentData;
    }

    public WebContentData getWebContent() {
        return webContent;
    }

}
