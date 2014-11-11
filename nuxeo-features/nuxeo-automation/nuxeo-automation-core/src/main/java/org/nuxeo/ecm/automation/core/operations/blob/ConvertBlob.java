/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ldoguin
 */
package org.nuxeo.ecm.automation.core.operations.blob;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.convert.ConvertHelper;

/**
 * Convert the given blob to a file with given mimetype.
 * 
 * @author ldoguin
 */
@Operation(id = ConvertBlob.ID, category = Constants.CAT_CONVERSION, label = "Convert to given mime-type", description = "Convert the input file to a file of the given mime-type and return the new file.", since = "5.7")
public class ConvertBlob {

    public static final String ID = "Blob.Convert";

    protected final ConvertHelper convertHelper = new ConvertHelper();

    @Context
    protected ConversionService service;

    @Param(name = "mimeType", required = true)
    protected String mimeType;

    @OperationMethod
    public Blob run(DocumentModel doc) throws Exception {
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh == null) {
            return null;
        }
        return run(bh.getBlob());
    }

    @OperationMethod
    public Blob run(Blob blob) throws Exception {
        Blob result = convertHelper.convertBlob(blob, mimeType);
        return result;
    }

    @OperationMethod
    public BlobList run(BlobList blobs) throws Exception {
        BlobList bl = new BlobList();
        for (Blob blob : blobs) {
            bl.add(this.run(blob));
        }
        return bl;
    }

}
