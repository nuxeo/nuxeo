/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stéphane Fourrier
 */

package org.nuxeo.opensocial.container.client.external;

import com.google.gwt.user.client.Random;

public class FileUtils {
    public static final String AUTOMATION_FILES_URL = "site/automation/files/";
    public static final String AUTOMATION_FILES_PATH_ATTR = "?path=%2Fcontent";

    public native static String getBaseUrl() /*-{
        return $wnd.nuxeo.baseURL;
    }-*/;

    public static String buildFileUrl(String id) {
        // TODO We use a hack, to be sure that the image will be reloaded.
        String rdmAttr = "&rdm=" + Random.nextInt(100000);
        return getBaseUrl() + AUTOMATION_FILES_URL + id + AUTOMATION_FILES_PATH_ATTR
                + rdmAttr;
    }
}
