/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.automation.jsf.operations;

import java.io.IOException;
import java.util.UUID;

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
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.tag.fn.Functions;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;

/**
 * @author Anahide Tchertchian
 */
@Operation(id = DownloadFile.ID, category = Constants.CAT_UI, requires = Constants.SEAM_CONTEXT, label = "Download file", description = "Download a file")
public class DownloadFile {

    protected static Log log = LogFactory.getLog(DownloadFile.class);

    public static final String ID = "Seam.DownloadFile";

    @Context
    protected OperationContext ctx;

    @OperationMethod
    public void run(Blob blob) throws Exception {
        if (blob == null) {
            throw new OperationException("there is no file content available");
        }

        FacesContext faces = FacesContext.getCurrentInstance();
        String filename = blob.getFilename();
        if (blob.getLength() > Functions.getBigFileSizeLimit()) {

            HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
            HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();

            if (!blob.isPersistent()) {
                blob.persist();
            }

            String sid = UUID.randomUUID().toString();
            request.getSession(true).setAttribute(sid, blob);

            String bigDownloadURL = BaseURL.getBaseURL(request);
            bigDownloadURL += "nxbigblob" + "/" + sid;

            try {
                // Operation was probably triggered by a POST
                // so we need to de-activate the ResponseWrapper that would
                // rewrite the URL
                request.setAttribute(
                        NXAuthConstants.DISABLE_REDIRECT_REQUEST_KEY,
                        new Boolean(true));
                // send the redirect
                response.sendRedirect(bigDownloadURL);
                // mark all JSF processing as completed
                response.flushBuffer();
                FacesContext.getCurrentInstance().responseComplete();
                // set Outcome to null (just in case)
                ctx.getVars().put("Outcome", null);
            } catch (IOException e) {
                log.error("Error while redirecting for big blob downloader", e);
            }
        } else {
            ComponentUtils.download(faces, blob, filename);
        }
    }

}
