package org.nuxeo.opensocial.container.client.rpc.layout.result;

import org.nuxeo.opensocial.container.shared.layout.api.YUIUnit;

import net.customware.gwt.dispatch.shared.Result;

/**
 * @author Stéphane Fourrier
 */
public class UpdateYUILayoutFooterResult implements Result {
    private static final long serialVersionUID = 1L;

    private YUIUnit footer;

    public UpdateYUILayoutFooterResult() {
    }

    public UpdateYUILayoutFooterResult(YUIUnit footer) {
        this.footer = footer;
    }

    public YUIUnit getFooter() {
        return footer;
    }
}
