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

package org.nuxeo.ecm.platform.api.login;

import java.io.Serializable;

import javax.security.auth.callback.Callback;

public class UserIdentificationInfoCallback implements Callback, Serializable {

    private static final long serialVersionUID = 5489765439867654390L;

    protected UserIdentificationInfo userInfo;

    public UserIdentificationInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserIdentificationInfo userInfo) {
        this.userInfo = userInfo;
    }

}
