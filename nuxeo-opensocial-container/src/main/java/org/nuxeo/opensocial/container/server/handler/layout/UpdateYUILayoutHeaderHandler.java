package org.nuxeo.opensocial.container.server.handler.layout;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.opensocial.container.client.rpc.layout.action.UpdateYUILayoutHeader;
import org.nuxeo.opensocial.container.client.rpc.layout.result.UpdateYUILayoutHeaderResult;
import org.nuxeo.opensocial.container.server.handler.AbstractActionHandler;
import org.nuxeo.opensocial.container.shared.layout.api.YUIUnit;

import net.customware.gwt.dispatch.server.ExecutionContext;

/**
 * @author Stéphane Fourrier
 */
public class UpdateYUILayoutHeaderHandler
        extends
        AbstractActionHandler<UpdateYUILayoutHeader, UpdateYUILayoutHeaderResult> {

    protected UpdateYUILayoutHeaderResult doExecute(
            UpdateYUILayoutHeader action, ExecutionContext context,
            CoreSession session) throws ClientException {
        YUIUnit header = getSpaceFromId(action.getSpaceId(), session).getLayout().setHeader(
                action.getHeader());
        return new UpdateYUILayoutHeaderResult(header);
    }

    public Class<UpdateYUILayoutHeader> getActionType() {
        return UpdateYUILayoutHeader.class;
    }

}
