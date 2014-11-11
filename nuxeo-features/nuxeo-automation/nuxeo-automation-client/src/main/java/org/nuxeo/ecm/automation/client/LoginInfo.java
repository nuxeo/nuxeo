/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    public LoginInfo(String username, Set<String> groups,
            boolean isAdministrator) {
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
