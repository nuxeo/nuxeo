package org.nuxeo.opensocial.container.server.handler.layout;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.opensocial.container.client.rpc.layout.action.UpdateYUILayoutFooter;
import org.nuxeo.opensocial.container.client.rpc.layout.result.UpdateYUILayoutFooterResult;
import org.nuxeo.opensocial.container.server.handler.AbstractActionHandler;
import org.nuxeo.opensocial.container.shared.layout.api.YUIUnit;

import net.customware.gwt.dispatch.server.ExecutionContext;

/**
 * @author Stéphane Fourrier
 */
public class UpdateYUILayoutFooterHandler
        extends
        AbstractActionHandler<UpdateYUILayoutFooter, UpdateYUILayoutFooterResult> {

    protected UpdateYUILayoutFooterResult doExecute(
            UpdateYUILayoutFooter action, ExecutionContext context,
            CoreSession session) throws ClientException {
        YUIUnit footer = getSpaceFromId(action.getSpaceId(), session).getLayout().setFooter(
                action.getFooter());
        return new UpdateYUILayoutFooterResult(footer);
    }

    public Class<UpdateYUILayoutFooter> getActionType() {
        return UpdateYUILayoutFooter.class;
    }

}
