/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.connect.client;

import org.nuxeo.connect.CallbackHolder;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.runtime.api.Framework;

/**
 * Provide access to Nuxeo Framework from Nuxeo Connect Client
 *
 * @author tiry
 */
public class NuxeoCallbackHolder implements CallbackHolder {

    @Override
    public String getHomePath() {
        return Framework.getRuntime().getHome().getAbsolutePath();
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return Framework.getProperty(key, defaultValue);
    }

    @Override
    public boolean isTestModeSet() {
        return Framework.isTestModeSet();
    }

    @Override
    public PackageUpdateService getUpdateService() {
        return Framework.getLocalService(PackageUpdateService.class);
    }

}
