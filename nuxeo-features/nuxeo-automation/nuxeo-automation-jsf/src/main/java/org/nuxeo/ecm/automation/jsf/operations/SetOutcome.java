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
 */

package org.nuxeo.ecm.automation.jsf.operations;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;

/**
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 * @deprecated Not used for now. To enable it add the operation to the XML
 *             contribution file.
 */
@Deprecated
@Operation(id = SetOutcome.ID, category = Constants.CAT_UI, requires = Constants.SEAM_CONTEXT, label = "Set JSF Outcome", description = "Set the 'Outcome' context variable that represent a JSF outcome string. This outcome can be used by the next operations that need an outcome. Preserve the current input (return back the same input).")
public class SetOutcome {

    public static final String ID = "Seam.SetOutcome";

    @Context
    protected OperationContext ctx;

    @Param(name = "outcome")
    protected String outcome;

    @OperationMethod
    public void run() throws Exception {
        ctx.put(SeamOperation.OUTCOME, outcome);
    }

}
