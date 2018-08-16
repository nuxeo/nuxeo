/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.automation.jsf.operations;

import java.io.IOException;
import java.util.Collections;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Anahide Tchertchian
 */
@Operation(id = DownloadFile.ID, category = Constants.CAT_UI, requires = Constants.SEAM_CONTEXT, label = "Download file", description = "Download a file", aliases = {
        "WebUI.DownloadFile" })
public class DownloadFile {

    protected static Log log = LogFactory.getLog(DownloadFile.class);

    public static final String ID = "Seam.DownloadFile";

    @Context
    protected OperationContext ctx;

    @OperationMethod
    public void run(Blob blob) throws OperationException, IOException {
        if (blob == null) {
            throw new OperationException("there is no file content available");
        }
        DownloadService downloadService = Framework.getService(DownloadService.class);
        String key = downloadService.storeBlobs(Collections.singletonList(blob));
        String url = BaseURL.getBaseURL() + downloadService.getDownloadUrl(key);

        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
        HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();

        try {
            // Operation was probably triggered by a POST
            // so we need to de-activate the ResponseWrapper that would
            // rewrite the URL
            request.setAttribute(NXAuthConstants.DISABLE_REDIRECT_REQUEST_KEY, new Boolean(true));
            // send the redirect
            externalContext.redirect(url);
            // mark all JSF processing as completed
            response.flushBuffer();
            FacesContext.getCurrentInstance().responseComplete();
            // set Outcome to null (just in case)
            ctx.getVars().put("Outcome", null);
        } catch (IOException e) {
            log.error("Error while redirecting for big blob downloader", e);
        }
    }

}
