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
package org.nuxeo.ecm.automation.jaxrs;

import java.util.Set;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class LoginInfo {

    protected final String id;

    protected String username;

    protected Set<String> groups;

    protected boolean isAdministrator;

    public LoginInfo(String username, Set<String> groups, boolean isAdministrator) {
        this(username, username, groups, isAdministrator);
    }

    /**
     * @since 2025.9
     */
    public LoginInfo(String id, String username, Set<String> groups, boolean isAdministrator) {
        this.id = id;
        this.username = username;
        this.groups = groups;
        this.isAdministrator = isAdministrator;
    }

    /**
     * Returns a unique identifier to use to reference the original principal externally.
     *
     * @return A unique identifier
     * @since 2025.9
     */
    public String getId() {
        return id;
    }

    public boolean isAdministrator() {
        return isAdministrator;
    }

    public String getUsername() {
        return username;
    }

    public Set<String> getGroups() {
        return groups;
    }
}
