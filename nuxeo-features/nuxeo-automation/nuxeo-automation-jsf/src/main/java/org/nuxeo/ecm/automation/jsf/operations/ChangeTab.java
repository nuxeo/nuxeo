/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.automation.jsf.operations;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.jsf.OperationHelper;

@Operation(id = ChangeTab.ID, category = Constants.CAT_UI, requires = Constants.SEAM_CONTEXT, label = "Change Tab", description = "Change the selected tab for the current document. Preserve the current input.")
public class ChangeTab {

    public static final String ID = "Seam.ChangeTab";

    @Context
    protected OperationContext ctx;

    @Param(name = "tab")
    protected String tab;

    @OperationMethod
    public void run() {
        OperationHelper.getWebActions().setCurrentTabId(tab);
    }

}
