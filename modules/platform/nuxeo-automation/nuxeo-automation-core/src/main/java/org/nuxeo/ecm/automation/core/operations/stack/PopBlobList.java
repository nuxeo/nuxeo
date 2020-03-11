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
@Operation(id = PopBlobList.ID, category = Constants.CAT_EXECUTION_STACK, label = "Pop File List", description = "Restore the last saved input file list in the context input stack", aliases = { "Blob.PopList" })
public class PopBlobList {

    public static final String ID = "Context.PopBlobList";

    @Context
    protected OperationContext ctx;

    @OperationMethod
    public BlobList run() throws OperationException {
        Object obj = ctx.pop(Constants.O_BLOBS);
        if (obj instanceof BlobList) {
            return (BlobList) obj;
        }
        throw new OperationException(
                "Illegal state error for pop file list operation. The context stack doesn't contains a file list on its top");
    }

}
