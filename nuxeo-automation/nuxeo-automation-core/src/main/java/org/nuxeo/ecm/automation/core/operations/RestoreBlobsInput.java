/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.operations;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = RestoreBlobsInput.ID, category = Constants.CAT_EXECUTION, label = "Restore Files Input", description = "Restore the file list input from a context variable given its name. Return the files.")
public class RestoreBlobsInput {

    public static final String ID = "Context.RestoreBlobsInput";

    @Context
    protected OperationContext ctx;

    @Param(name = "name")
    protected String name;

    @OperationMethod
    public BlobList run() throws Exception {
        Object obj = ctx.get(name);
        if (obj instanceof BlobList) {
            return (BlobList) obj;
        } else if (obj instanceof Blob) {
            return new BlobList((Blob) obj);
        }
        throw new OperationException(
                "Illegal state error for restore files operation. The context map doesn't contains a file list variable with name "
                        + name);
    }

}
