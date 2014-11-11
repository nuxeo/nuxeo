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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.service;

import java.util.List;

/**
 * @author Alexandre Russel
 *
 */
public class URLPatternFilter {

    private final boolean allowDeny;

    private final List<String> denies;

    private final List<String> allows;

    public URLPatternFilter(boolean allowDeny, List<String> denies,
            List<String> allows) {
        this.allowDeny = allowDeny;
        this.denies = denies;
        this.allows = allows;
    }

    public boolean allow(String url) {
        if (checkFirstPass(url) && !checkSecondPass(url)) {
            return allowDeny;
        }
        return !allowDeny;
    }

    private boolean checkSecondPass(String url) {
        return checkMatch(allowDeny ? denies : allows, url);
    }

    private static boolean checkMatch(List<String> list, String url) {
        for (String regex : list) {
            if (url.matches(regex)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkFirstPass(String url) {
        return checkMatch(allowDeny ? allows : denies, url);
    }

}
