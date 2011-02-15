package org.nuxeo.opensocial.container.server.handler.webcontent;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.opensocial.container.client.rpc.webcontent.action.UpdateWebContent;
import org.nuxeo.opensocial.container.client.rpc.webcontent.result.UpdateWebContentResult;
import org.nuxeo.opensocial.container.server.handler.AbstractActionHandler;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;

import net.customware.gwt.dispatch.server.ExecutionContext;

/**
 * @author Stéphane Fourrier
 */
public class UpdateWebContentHandler extends
        AbstractActionHandler<UpdateWebContent, UpdateWebContentResult> {

    protected UpdateWebContentResult doExecute(UpdateWebContent action,
            ExecutionContext context, CoreSession session)
            throws ClientException {
        Space space = getSpaceFromId(action.getSpaceId(), session);
        WebContentData webContent = action.getWebContent();
        WebContentData data = updateWebContent(webContent, space);

        return new UpdateWebContentResult(data);
    }

    public static WebContentData updateWebContent(WebContentData webContent,
            Space space) throws ClientException {
        WebContentData old = space.getWebContent(webContent.getId());

        /*
         * Updates content is made of two parts, because of the fact that unitId
         * is stored in the webContent : - update the content metadata - move
         * the content to another unit if needed
         */
        WebContentData data = space.updateWebContent(webContent);

        String dstUnitId = webContent.getUnitId();
        if (!old.getUnitId().equals(dstUnitId)) {
            space.moveWebContent(old, dstUnitId);
        }

        return data;
    }

    public Class<UpdateWebContent> getActionType() {
        return UpdateWebContent.class;
    }

}
