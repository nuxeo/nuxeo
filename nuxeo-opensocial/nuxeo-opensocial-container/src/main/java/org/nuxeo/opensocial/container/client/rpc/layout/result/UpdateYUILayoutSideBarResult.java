package org.nuxeo.opensocial.container.client.rpc.layout.result;

import org.nuxeo.opensocial.container.shared.layout.api.YUIUnit;

import net.customware.gwt.dispatch.shared.Result;

/**
 * @author Stéphane Fourrier
 */
public class UpdateYUILayoutSideBarResult implements Result {
    private static final long serialVersionUID = 1L;

    private YUIUnit sideBar;

    public UpdateYUILayoutSideBarResult() {
    }

    public UpdateYUILayoutSideBarResult(YUIUnit sideBar) {
        this.sideBar = sideBar;
    }

    public YUIUnit getSideBar() {
        return sideBar;
    }
}
