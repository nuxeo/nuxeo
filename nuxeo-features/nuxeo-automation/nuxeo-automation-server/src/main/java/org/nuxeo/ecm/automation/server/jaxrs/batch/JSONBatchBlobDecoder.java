/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.automation.server.jaxrs.batch;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.node.ObjectNode;
import org.nuxeo.ecm.automation.core.util.JSONBlobDecoder;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestCleanupHandler;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestContext;
import org.nuxeo.runtime.api.Framework;

/**
 * Uses a JSON definition to retrive a Blob uploaded in a batch
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public class JSONBatchBlobDecoder implements JSONBlobDecoder {

    @Override
    public Blob getBlobFromJSON(ObjectNode jsonObject) {

        Blob blob = null;

        if (!jsonObject.has("upload-batch")) {
            return null;
        }

        final String batchId = jsonObject.get("upload-batch").getTextValue();
        String fileId = null;
        if (jsonObject.has("upload-fileId")) {
            fileId = jsonObject.get("upload-fileId").getTextValue();
        }
        if (fileId!=null) {
            BatchManager bm = Framework.getLocalService(BatchManager.class);
            blob = bm.getBlob(batchId, fileId);

            if (RequestContext.getActiveContext()!=null) {
                RequestContext.getActiveContext().addRequestCleanupHandler(new RequestCleanupHandler() {
                    @Override
                    public void cleanup(HttpServletRequest request) {
                        BatchManager bm = Framework.getLocalService(BatchManager.class);
                        bm.clean(batchId);
                    }
                });
            }

        }
        return blob;
    }

}
