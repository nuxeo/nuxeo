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
import org.nuxeo.ecm.automation.jsf.OperationHelper;

@Operation(id = CreateDocumentForm.ID, category = Constants.CAT_UI, requires = Constants.SEAM_CONTEXT, label = "Show Create Document Page", description = "Show the document creation form given a type. This is a void operation - the input object is returned back as the oputput")
public class CreateDocumentForm {

    public static final String ID = "Seam.CreateDocumentForm";

    @Context
    protected OperationContext ctx;

    @Param(name = "type")
    protected String type;

    @OperationMethod
    public void run() throws Exception {
        ctx.put(SeamOperation.OUTCOME,
                OperationHelper.getDocumentActions().createDocument(type));
    }

}
