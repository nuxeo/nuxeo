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
package org.nuxeo.ecm.automation.core.operations.stack;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.util.BlobList;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = PopBlobList.ID, category = Constants.CAT_EXECUTION_STACK, label = "Pop File List", description = "Restore the last saved input file list in the context input stack")
public class PopBlobList {

    public static final String ID = "Blob.PopList";

    @Context
    protected OperationContext ctx;

    @OperationMethod
    public BlobList run() throws Exception {
        Object obj = ctx.pop(Constants.O_BLOBS);
        if (obj instanceof BlobList) {
            return (BlobList) obj;
        }
        throw new OperationException(
                "Illegal state error for pop file list operation. The context stack doesn't contains a file list on its top");
    }

}
