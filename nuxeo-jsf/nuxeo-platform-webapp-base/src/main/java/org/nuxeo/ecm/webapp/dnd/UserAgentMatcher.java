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
 */

package org.nuxeo.ecm.webapp.dnd;

import java.util.regex.Pattern;

/**
 * Helper class to detect Html5 Dnd compliant browsers based on the User Agent
 * string
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public class UserAgentMatcher {

    private static final Pattern UA_FIREFOX = Pattern.compile("^[Mm]ozilla.*[Ff]irefox(/|\\s)?((3\\.[6789].*)|([456789].*))");

    private static final Pattern UA_SAFARI = Pattern.compile("^Mozilla.*AppleWebKit.*Version/5.*");

    private static final Pattern UA_CHROME = Pattern.compile("^Mozilla.*AppleWebKit.*Chrom(e|ium)/(1[0123456789]).*");

    public static boolean html5DndIsSupported(String UA) {

        if (UA_FIREFOX.matcher(UA).matches()) {
            return true;
        }

        if (UA_SAFARI.matcher(UA).matches()) {
            return true;
        }

        if (UA_CHROME.matcher(UA).matches()) {
            return true;
        }

        return false;
    }

}
