package org.nuxeo.opensocial.container.server.handler.layout;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.opensocial.container.client.rpc.layout.action.UpdateYUILayoutBodySize;
import org.nuxeo.opensocial.container.client.rpc.layout.result.UpdateYUILayoutBodySizeResult;
import org.nuxeo.opensocial.container.server.handler.AbstractActionHandler;

import net.customware.gwt.dispatch.server.ExecutionContext;

/**
 * @author Stéphane Fourrier
 */
public class UpdateYUILayoutBodySizeHandler
        extends
        AbstractActionHandler<UpdateYUILayoutBodySize, UpdateYUILayoutBodySizeResult> {

    protected UpdateYUILayoutBodySizeResult doExecute(
            UpdateYUILayoutBodySize action, ExecutionContext context,
            CoreSession session) throws ClientException {
        getSpaceFromId(action.getSpaceId(), session).getLayout().setBodySize(
                action.getBodySize());
        return new UpdateYUILayoutBodySizeResult();
    }

    public Class<UpdateYUILayoutBodySize> getActionType() {
        return UpdateYUILayoutBodySize.class;
    }

}
