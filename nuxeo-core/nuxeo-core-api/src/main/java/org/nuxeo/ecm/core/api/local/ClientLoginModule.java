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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.local;

import java.security.Principal;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.runtime.api.login.LoginComponent;

/**
 * @deprecated since 11.1, use {@link LoginComponent} instead
 */
@Deprecated
public class ClientLoginModule {

    /**
     * @since 5.7
     * @deprecated since 11.1, use {@link LoginComponent#clearPrincipalStack} instead
     */
    @Deprecated
    public static void clearThreadLocalLogin() {
        LoginComponent.clearPrincipalStack();
    }

    /**
     * @deprecated since 11.1, use {@link LoginComponent} instead
     */
    @Deprecated
    public static LoginStack getThreadLocalLogin() {
        return new LoginStack(); // delegates to LoginComponent's stack
    }

    /**
     * @deprecated since 11.1, use {@link LoginComponent#getCurrentPrincipal} instead
     */
    @Deprecated
    public static LoginStack.Entry getCurrentLogin() {
        Principal principal = LoginComponent.getCurrentPrincipal();
        return principal == null ? null : new LoginStack.Entry(principal, null, null);
    }

    /**
     * Returns the current logged {@link NuxeoPrincipal} from the login stack
     *
     * @since 5.6
     * @deprecated since 11.1, use {@link NuxeoPrincipal#getCurrent} instead
     */
    @Deprecated
    public static NuxeoPrincipal getCurrentPrincipal() {
        return NuxeoPrincipal.getCurrent();
    }

    /**
     * @since 11.1
     * @deprecated since 11.1, use {@link NuxeoPrincipal#isCurrentAdministrator} instead
     */
    @Deprecated
    public static boolean isCurrentAdministrator() {
        return NuxeoPrincipal.isCurrentAdministrator();
    }

}
