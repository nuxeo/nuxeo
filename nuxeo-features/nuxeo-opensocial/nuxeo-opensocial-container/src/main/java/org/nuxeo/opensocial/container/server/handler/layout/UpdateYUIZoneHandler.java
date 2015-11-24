package org.nuxeo.opensocial.container.server.handler.layout;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.opensocial.container.client.rpc.layout.action.UpdateYUIZone;
import org.nuxeo.opensocial.container.client.rpc.layout.result.UpdateYUIZoneResult;
import org.nuxeo.opensocial.container.server.handler.AbstractActionHandler;
import org.nuxeo.opensocial.container.shared.layout.api.YUIComponentZone;

import net.customware.gwt.dispatch.server.ExecutionContext;

/**
 * @author Stéphane Fourrier
 */
public class UpdateYUIZoneHandler extends
        AbstractActionHandler<UpdateYUIZone, UpdateYUIZoneResult> {

    protected UpdateYUIZoneResult doExecute(UpdateYUIZone action,
            ExecutionContext context, CoreSession session)
            throws ClientException {
        YUIComponentZone zone = getSpaceFromId(action.getSpaceId(), session).getLayout().updateZone(
                action.getZone(), action.getZoneIndex(), action.getTemplate());
        return new UpdateYUIZoneResult(zone);
    }

    public Class<UpdateYUIZone> getActionType() {
        return UpdateYUIZone.class;
    }

}
