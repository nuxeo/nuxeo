package org.nuxeo.opensocial.container.client.rpc.layout.action;

import org.nuxeo.opensocial.container.client.rpc.AbstractAction;
import org.nuxeo.opensocial.container.client.rpc.ContainerContext;
import org.nuxeo.opensocial.container.client.rpc.layout.result.UpdateYUILayoutFooterResult;
import org.nuxeo.opensocial.container.shared.layout.api.YUIUnit;

/**
 * @author Stéphane Fourrier
 */
public class UpdateYUILayoutFooter extends
        AbstractAction<UpdateYUILayoutFooterResult> {

    private static final long serialVersionUID = 1L;

    private YUIUnit footer;

    @SuppressWarnings("unused")
    private UpdateYUILayoutFooter() {
        super();
    }

    public UpdateYUILayoutFooter(ContainerContext containerContext,
            final YUIUnit footer) {
        super(containerContext);
        this.footer = footer;
    }

    public YUIUnit getFooter() {
        return footer;
    }

}
