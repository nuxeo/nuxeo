/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *         Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.webapp.security;

import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 8.2
 */
public class UpdateProfilePasswordUnrestricted extends UnrestrictedSessionRunner {

    private final String newPassword;

    private final String username;

    private final String oldPassword;

    public UpdateProfilePasswordUnrestricted(String defaultRepositoryName, String username, String oldPassword,
            String newPassword) {
        super(defaultRepositoryName);
        this.newPassword = newPassword;
        this.username = username;
        this.oldPassword = oldPassword;
    }

    @Override
    public void run() {
        UserManager userManager = Framework.getService(UserManager.class);
        userManager.updatePassword(username, oldPassword, newPassword);
    }

}
