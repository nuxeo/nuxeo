package org.nuxeo.opensocial.container.client.rpc.layout.action;

import org.nuxeo.opensocial.container.client.rpc.AbstractAction;
import org.nuxeo.opensocial.container.client.rpc.ContainerContext;
import org.nuxeo.opensocial.container.client.rpc.layout.result.UpdateYUILayoutHeaderResult;
import org.nuxeo.opensocial.container.shared.layout.api.YUIUnit;

/**
 * @author Stéphane Fourrier
 */
public class UpdateYUILayoutHeader extends
        AbstractAction<UpdateYUILayoutHeaderResult> {

    private static final long serialVersionUID = 1L;

    private YUIUnit header;

    @SuppressWarnings("unused")
    private UpdateYUILayoutHeader() {
        super();
    }

    public UpdateYUILayoutHeader(ContainerContext containerContext,
            final YUIUnit header) {
        super(containerContext);
        this.header = header;
    }

    public YUIUnit getHeader() {
        return header;
    }

}
