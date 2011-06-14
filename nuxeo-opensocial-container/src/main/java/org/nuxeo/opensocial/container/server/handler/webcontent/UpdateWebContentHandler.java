/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Stéphane Fourrier
 */

package org.nuxeo.opensocial.container.server.handler.webcontent;

import java.io.Serializable;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.gwt.habyt.upload.server.UploadServlet;
import org.nuxeo.gwt.habyt.upload.server.UploadedFile;
import org.nuxeo.gwt.habyt.upload.server.UploadedFileManager;
import org.nuxeo.opensocial.container.client.rpc.webcontent.action.UpdateWebContent;
import org.nuxeo.opensocial.container.client.rpc.webcontent.result.UpdateWebContentResult;
import org.nuxeo.opensocial.container.server.handler.AbstractActionHandler;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;

import com.google.inject.Inject;
import com.google.inject.Provider;

import net.customware.gwt.dispatch.server.ExecutionContext;

/**
 * @author Stéphane Fourrier
 */
public class UpdateWebContentHandler extends
        AbstractActionHandler<UpdateWebContent, UpdateWebContentResult> {

    protected static Provider<HttpServletRequest> requestProvider;

     @Inject
     public UpdateWebContentHandler(Provider<HttpServletRequest> requestProvider) {
         UpdateWebContentHandler.requestProvider = requestProvider;
     }

    protected UpdateWebContentResult doExecute(UpdateWebContent action,
            ExecutionContext context, CoreSession session)
            throws ClientException {
        Space space = getSpaceFromId(action.getSpaceId(), session);
        WebContentData webContent = action.getWebContent();
        List<String> files = action.getFiles();
        WebContentData data = updateWebContent(webContent, files, space);

        return new UpdateWebContentResult(data);
    }

    public static WebContentData updateWebContent(WebContentData webContent,
            List<String> filesIds, Space space) throws ClientException {
        WebContentData old = space.getWebContent(webContent.getId());
         WebContentData data = null;

        /*
         * Updates content is made of two parts, because of the fact that unitId
         * is stored in the webContent : - update the content metadata - move
         * the content to another unit if needed
         */

         if (filesIds != null && webContent.hasFiles()) {
             UploadedFileManager mgr = UploadServlet.getUploadedFileManager(requestProvider.get());
             for (String fileId : filesIds) {
                 UploadedFile uploadedFile = (UploadedFile) mgr.get(fileId);
                 if (uploadedFile != null) {
                     mgr.remove(fileId);
                     webContent.addFile((Serializable) getBlob(uploadedFile.getFile()));
                 } else {
                     // TODO a file has been uploaded but is not in the http
                     // session
                 }
             }
             data = space.updateWebContent(webContent);
         } else {
             data = space.updateWebContent(webContent);
         }


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
