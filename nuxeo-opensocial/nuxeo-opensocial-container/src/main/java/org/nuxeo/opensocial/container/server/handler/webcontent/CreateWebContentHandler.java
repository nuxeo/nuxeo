package org.nuxeo.opensocial.container.server.handler.webcontent;

import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.opensocial.container.client.rpc.webcontent.action.CreateWebContent;
import org.nuxeo.opensocial.container.client.rpc.webcontent.result.CreateWebContentResult;
import org.nuxeo.opensocial.container.server.handler.AbstractActionHandler;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;

import net.customware.gwt.dispatch.server.ExecutionContext;

/**
 * @author Stéphane Fourrier
 */
public class CreateWebContentHandler extends
        AbstractActionHandler<CreateWebContent, CreateWebContentResult> {
    protected CreateWebContentResult doExecute(CreateWebContent action,
            ExecutionContext context, CoreSession session)
            throws ClientException {
        String spaceId = action.getSpaceId();
        Space space = getSpaceFromId(spaceId, session);
        WebContentData data = space.createWebContent(action.getData());
        Map<String, Boolean> permissions = space.getPermissions(spaceId);
        return new CreateWebContentResult(data, permissions);
    }

    public Class<CreateWebContent> getActionType() {
        return CreateWebContent.class;
    }

}
