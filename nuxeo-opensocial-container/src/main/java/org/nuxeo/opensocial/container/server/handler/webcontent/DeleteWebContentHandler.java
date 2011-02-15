package org.nuxeo.opensocial.container.server.handler.webcontent;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.opensocial.container.client.rpc.webcontent.action.DeleteWebContent;
import org.nuxeo.opensocial.container.client.rpc.webcontent.result.DeleteWebContentResult;
import org.nuxeo.opensocial.container.server.handler.AbstractActionHandler;

import net.customware.gwt.dispatch.server.ExecutionContext;

/**
 * @author Stéphane Fourrier
 */
public class DeleteWebContentHandler extends
        AbstractActionHandler<DeleteWebContent, DeleteWebContentResult> {
    protected DeleteWebContentResult doExecute(DeleteWebContent action,
            ExecutionContext context, CoreSession session)
            throws ClientException {
        getSpaceFromId(action.getSpaceId(), session).deleteWebContent(
                action.getData());
        return new DeleteWebContentResult();
    }

    public Class<DeleteWebContent> getActionType() {
        return DeleteWebContent.class;
    }
}
