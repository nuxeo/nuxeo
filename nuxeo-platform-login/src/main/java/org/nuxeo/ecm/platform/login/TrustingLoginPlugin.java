/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.login;

import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;

/**
 * Dummy LoginModule Plugin:
 * it always trusts in the UserIndetificationInfo that it receive
 * and does no validation.
 * <p>
 * This plugin can be used when the complete authentication process
 * has already been done before: for example by an authentification proxy.
 * <p>
 * Warning: only configure this LoginModulePlugin if you have an Auth Plugin
 * that checks completely user authentication.
 *
 * @author tiry
 *
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
