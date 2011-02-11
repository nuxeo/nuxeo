package org.nuxeo.opensocial.container.server.handler.layout;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.opensocial.container.client.rpc.layout.action.CreateYUIZone;
import org.nuxeo.opensocial.container.client.rpc.layout.result.CreateYUIZoneResult;
import org.nuxeo.opensocial.container.server.handler.AbstractActionHandler;
import org.nuxeo.opensocial.container.shared.layout.api.YUIComponentZone;

import net.customware.gwt.dispatch.server.ExecutionContext;

/**
 * @author Stéphane Fourrier
 */
public class CreateYUIZoneHandler extends
        AbstractActionHandler<CreateYUIZone, CreateYUIZoneResult> {
    protected CreateYUIZoneResult doExecute(CreateYUIZone action,
            ExecutionContext context, CoreSession session)
            throws ClientException {
        YUIComponentZone zone = getSpaceFromId(action.getSpaceId(), session).getLayout().createZone(
                action.getZone(), action.getZoneIndex());
        return new CreateYUIZoneResult(zone);
    }

    public Class<CreateYUIZone> getActionType() {
        return CreateYUIZone.class;
    }

}
