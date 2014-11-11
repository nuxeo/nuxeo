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
import org.nuxeo.ecm.core.api.Blob;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = PullBlob.ID, category = Constants.CAT_EXECUTION_STACK, label = "Pull File", description = "Restore the last saved input file in the context input stack. This operation must be used only if a PUSH operation was previously made. Return the first <i>pushed</i> file.")
public class PullBlob {

    public static final String ID = "Blob.Pull";

    @Context
    protected OperationContext ctx;

    @OperationMethod
    public Blob run() throws Exception {
        Object obj = ctx.pull(Constants.O_BLOB);
        if (obj instanceof Blob) {
            return (Blob) obj;
        }
        throw new OperationException(
                "Illegal state error for pull file operation. The context stack doesn't contains a file on its bottom");
    }

}
