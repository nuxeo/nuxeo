/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.service;

import java.util.List;

/**
 * @author Alexandre Russel
 */
public class URLPatternFilter {

    private final boolean allowDeny;

    private final List<String> denies;

    private final List<String> allows;

    public URLPatternFilter(boolean allowDeny, List<String> denies, List<String> allows) {
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
