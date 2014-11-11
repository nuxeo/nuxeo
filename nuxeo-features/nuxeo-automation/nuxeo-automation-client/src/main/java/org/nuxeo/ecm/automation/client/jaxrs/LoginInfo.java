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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.jaxrs;

import java.util.Collections;
import java.util.Set;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class LoginInfo {

    public final static LoginInfo ANONYNMOUS = new LoginInfo("Anonymous");

    protected String username;
    protected Set<String> groups;
    protected boolean isAdministrator;

    public LoginInfo(String username) {
        this (username, null);
    }

    public LoginInfo(String username, Set<String> groups) {
        this (username, groups, false);
    }

    public LoginInfo(String username, Set<String> groups, boolean isAdministrator) {
        this.username = username;
        this.isAdministrator = isAdministrator;
        if (groups == null) {
            this.groups = Collections.emptySet();
        } else {
            this.groups = groups;
        }
    }

    public boolean isAdministrator() {
        return isAdministrator;
    }

    public String getUsername() {
        return username;
    }

    public String[] getGroups() {
        return groups.toArray(new String[groups.size()]);
    }

    public boolean hasGroup(String group) {
        return groups.contains(group);
    }

}
