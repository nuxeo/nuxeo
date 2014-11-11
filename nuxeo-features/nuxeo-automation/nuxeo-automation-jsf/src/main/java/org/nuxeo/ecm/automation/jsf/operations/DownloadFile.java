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

import javax.faces.context.FacesContext;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;

/**
 * @author Anahide Tchertchian
 */
@Operation(id = DownloadFile.ID, category = Constants.CAT_UI, requires = Constants.SEAM_CONTEXT, label = "Download file", description = "Download a file")
public class DownloadFile {

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
        ComponentUtils.download(faces, blob, filename);
    }

}
