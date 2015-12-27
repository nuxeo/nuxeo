/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@SuppressWarnings("serial")
public class LoginInfo implements Serializable {

    public static final LoginInfo ANONYNMOUS = new LoginInfo("Anonymous");

    protected String username;

    protected Set<String> groups;

    protected boolean isAdministrator;

    public LoginInfo(String username) {
        this(username, null);
    }

    public LoginInfo(String username, Set<String> groups) {
        this(username, groups, false);
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
