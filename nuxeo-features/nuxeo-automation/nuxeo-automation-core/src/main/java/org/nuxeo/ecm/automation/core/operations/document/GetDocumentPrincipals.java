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
package org.nuxeo.ecm.automation.core.operations.document;

import java.util.Set;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.PrincipalHelper;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.PermissionProvider;
import org.nuxeo.ecm.platform.usermanager.UserManager;

/**
 * Save the input document
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@Operation(id=GetDocumentPrincipals.ID, category=Constants.CAT_DOCUMENT, label="Get Principals",
        description="Fetch the principals that have a given permission on the input document and then set them in the context under the given key variable name. The operation returns the input document. You can later use the list of principals set by this operation on the context from another operation. The 'key' arument represent the variable name and the 'permission' argument the perission to check. If 'resolve groups' argument is true then groups are recusively resolved. Be <b>warned</b> that this may be a very consuming operation.<ul>Note that <li></li><li>groups are not included</li><li>the list pushed into the context is a set of nuxeo principals.</li></ul>")
public class GetDocumentPrincipals {

    public static final String ID = "Document.GetPrincipal";

    protected @Context PermissionProvider permissionProvider;
    protected @Context UserManager umgr;
    protected @Context OperationContext ctx;
    @Param(name="permission")
    protected String permission;
    @Param(name="key")
    protected String key;
    @Param(name="resolve groups", required=false, values="false")
    protected boolean resolveGroups = false;


    @OperationMethod
    public DocumentModel run(DocumentModel input) throws Exception {
        PrincipalHelper ph = new PrincipalHelper(umgr, permissionProvider);
        Set<NuxeoPrincipal> result = ph.getPrincipalsForPermission(input, permission, resolveGroups);
        ctx.put(key, result);
        return input;
    }

}
