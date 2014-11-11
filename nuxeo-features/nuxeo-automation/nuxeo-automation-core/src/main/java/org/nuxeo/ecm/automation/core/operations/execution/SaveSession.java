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
package org.nuxeo.ecm.automation.core.operations.execution;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 * Save the session - TODO remove this?
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = SaveSession.ID, category = Constants.CAT_SUBCHAIN_EXECUTION, label = "Save Session", description = "Commit any changes made by the operation on the documents. This can be used to explicitely commit changes. This operation can be executed on any type of input. The input of this operation will be preserved as the input for the next operation in the chain.")
public class SaveSession {

    public static final String ID = "Document.SaveSession";

    @Context
    protected CoreSession session;

    @OperationMethod
    public void run() throws Exception {
        session.save();
    }

}
