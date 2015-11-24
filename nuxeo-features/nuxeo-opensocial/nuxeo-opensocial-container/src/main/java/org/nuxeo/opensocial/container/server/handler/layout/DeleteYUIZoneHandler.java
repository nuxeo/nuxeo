package org.nuxeo.opensocial.container.server.handler.layout;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.opensocial.container.client.rpc.layout.action.DeleteYUIZone;
import org.nuxeo.opensocial.container.client.rpc.layout.result.DeleteYUIZoneResult;
import org.nuxeo.opensocial.container.server.handler.AbstractActionHandler;

import net.customware.gwt.dispatch.server.ExecutionContext;

/**
 * @author Stéphane Fourrier
 */
public class DeleteYUIZoneHandler extends
        AbstractActionHandler<DeleteYUIZone, DeleteYUIZoneResult> {

    protected DeleteYUIZoneResult doExecute(DeleteYUIZone action,
            ExecutionContext context, CoreSession session)
            throws ClientException {
        getSpaceFromId(action.getSpaceId(), session).getLayout().deleteZone(
                action.getZoneIndex());
        return new DeleteYUIZoneResult();
    }

    public Class<DeleteYUIZone> getActionType() {
        return DeleteYUIZone.class;
    }
}
