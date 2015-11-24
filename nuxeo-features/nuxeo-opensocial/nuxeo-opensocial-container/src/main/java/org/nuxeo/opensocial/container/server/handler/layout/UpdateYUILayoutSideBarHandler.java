package org.nuxeo.opensocial.container.server.handler.layout;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.opensocial.container.client.rpc.layout.action.UpdateYUILayoutSideBar;
import org.nuxeo.opensocial.container.client.rpc.layout.result.UpdateYUILayoutSideBarResult;
import org.nuxeo.opensocial.container.server.handler.AbstractActionHandler;
import org.nuxeo.opensocial.container.shared.layout.api.YUIUnit;

import net.customware.gwt.dispatch.server.ExecutionContext;

/**
 * @author Stéphane Fourrier
 */
public class UpdateYUILayoutSideBarHandler
        extends
        AbstractActionHandler<UpdateYUILayoutSideBar, UpdateYUILayoutSideBarResult> {

    protected UpdateYUILayoutSideBarResult doExecute(
            UpdateYUILayoutSideBar action, ExecutionContext context,
            CoreSession session) throws ClientException {
        YUIUnit sideBar = getSpaceFromId(action.getSpaceId(), session).getLayout().setSideBar(
                action.getSidebar());
        return new UpdateYUILayoutSideBarResult(sideBar);
    }

    public Class<UpdateYUILayoutSideBar> getActionType() {
        return UpdateYUILayoutSideBar.class;
    }

}
