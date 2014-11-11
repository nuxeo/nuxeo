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
@Operation(id = PushToSeamContext.ID, category = Constants.CAT_UI, requires=Constants.SEAM_CONTEXT,
        label = "Push to Seam Context", description = "Push the current input document into Seam context. Returns back the document.")
public class PushToSeamContext {

    public static final String ID = "Seam.PushDocument";


    protected @Context OperationContext ctx;
    protected @Param(name="name") String name;
    protected @Param(name="scope", widget=Constants.W_OPTION, values={"session", "conversation", "page", "event"}) String scope;

    @OperationMethod
    public DocumentModel push(DocumentModel value) throws Exception {

        if ("session".equalsIgnoreCase(scope)) {
            Contexts.getSessionContext().set(name, value);
        }
        else if ("conversation".equalsIgnoreCase(scope)) {
            Contexts.getConversationContext().set(name, value);
        }
        else if ("page".equalsIgnoreCase(scope)) {
            Contexts.getPageContext().set(name, value);
        }
        else if ("event".equalsIgnoreCase(scope)) {
            Contexts.getEventContext().set(name, value);
        }

        return value;
    }


}
