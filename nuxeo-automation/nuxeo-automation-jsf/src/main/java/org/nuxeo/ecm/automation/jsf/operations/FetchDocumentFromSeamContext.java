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

import org.jboss.seam.contexts.Contexts;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 *
 */
@Operation(id = FetchDocumentFromSeamContext.ID, category = Constants.CAT_FETCH, requires=Constants.SEAM_CONTEXT,
        label = "UI Document From Seam", description = "Fetch a document from the Seam context given its Seam name.")
public class FetchDocumentFromSeamContext {

    public static final String ID = "Seam.FetchDocument";

    protected @Context OperationContext ctx;
    protected @Param(name="name") String name;

    @OperationMethod
    public DocumentModel run() throws Exception {
        return (DocumentModel) Contexts.lookupInStatefulContexts(name);
    }

}
