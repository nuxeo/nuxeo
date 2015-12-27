/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.login;

import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;

/**
 * Dummy LoginModule Plugin: it always trusts in the UserIndetificationInfo that it receive and does no validation.
 * <p>
 * This plugin can be used when the complete authentication process has already been done before: for example by an
 * authentification proxy.
 * <p>
 * Warning: only configure this LoginModulePlugin if you have an Auth Plugin that checks completely user authentication.
 *
 * @author tiry
 */
public class TrustingLoginPlugin extends BaseLoginModule {

    /**
     * The name associated to this LoginPlugin.
     */
    public static final String NAME = "Trusting_LM";

    public Boolean initLoginModule() {
        return Boolean.TRUE;
    }

    public String validatedUserIdentity(UserIdentificationInfo userIdent) {
        return userIdent.getUserName();
    }

}
