/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.browse;

import org.nuxeo.apidoc.security.SecurityConstants;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.webengine.model.WebContext;

public class SecurityHelper {

    public static boolean canEditDocumentation(WebContext ctx) {
        NuxeoPrincipal principal = ctx.getPrincipal();
        return canEditDocumentation(principal);
    }

    public static boolean canEditDocumentation(NuxeoPrincipal principal) {

        if (principal.isAdministrator()) {
            return true;
        }
        if (principal.isAnonymous()) {
            return false;
        }
        return principal.getAllGroups().contains(SecurityConstants.Write_Group);

    }

}
