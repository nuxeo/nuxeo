package org.nuxeo.opensocial.container.client.rpc.layout.action;

import org.nuxeo.opensocial.container.client.rpc.AbstractAction;
import org.nuxeo.opensocial.container.client.rpc.ContainerContext;
import org.nuxeo.opensocial.container.client.rpc.layout.result.UpdateYUILayoutSideBarResult;
import org.nuxeo.opensocial.container.shared.layout.enume.YUISideBarStyle;

/**
 * @author Stéphane Fourrier
 */
public class UpdateYUILayoutSideBar extends
        AbstractAction<UpdateYUILayoutSideBarResult> {

    private static final long serialVersionUID = 1L;

    private YUISideBarStyle sideBar;

    @SuppressWarnings("unused")
    private UpdateYUILayoutSideBar() {
        super();
    }

    public UpdateYUILayoutSideBar(ContainerContext containerContext,
            final YUISideBarStyle sideBar) {
        super(containerContext);
        this.sideBar = sideBar;
    }

    public YUISideBarStyle getSidebar() {
        return sideBar;
    }

}
