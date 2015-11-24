package org.nuxeo.opensocial.container.client.rpc.layout.action;

import org.nuxeo.opensocial.container.client.rpc.AbstractAction;
import org.nuxeo.opensocial.container.client.rpc.ContainerContext;
import org.nuxeo.opensocial.container.client.rpc.layout.result.UpdateYUILayoutBodySizeResult;
import org.nuxeo.opensocial.container.shared.layout.api.YUIBodySize;

/**
 * @author Stéphane Fourrier
 */
public class UpdateYUILayoutBodySize extends
        AbstractAction<UpdateYUILayoutBodySizeResult> {

    private static final long serialVersionUID = 1L;

    private YUIBodySize size;

    @SuppressWarnings("unused")
    private UpdateYUILayoutBodySize() {
        super();
    }

    public UpdateYUILayoutBodySize(ContainerContext containerContext,
            final YUIBodySize size) {
        super(containerContext);
        this.size = size;
    }

    public YUIBodySize getBodySize() {
        return size;
    }

}
