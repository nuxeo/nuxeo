/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 */
@Operation(id=PopBlob.ID, category=Constants.CAT_EXECUTION_STACK, label="Pop File",
    description="Restore the last saved input file in the context input stack. This operation must be used only if a PUSH operation was previously made. Return the last <i>pushed</i> file.")
public class PopBlob {

    public final static String ID = "Blob.Pop";

    protected @Context OperationContext ctx;

    @OperationMethod
    public Blob run() throws Exception {
        Object obj = ctx.pop(Constants.O_BLOB);
        if (obj instanceof Blob) {
            return (Blob)obj;
        }
        throw new OperationException("Illegal state error for pop file operation. The context stack doesn't contains a file on its top");
    }

}
