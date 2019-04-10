/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */

package org.nuxeo.wss.spi.dws;

public class UserImpl implements User {

    protected String id;
    protected String login;
    protected String name;
    protected String email;
    protected boolean domainGroup = false;
    protected boolean siteAdmin = false;

    public UserImpl(String id, String login, String name, String email) {
        this.id = id;
        this.login = login;
        this.name = name;
        this.email = email;
    }

    public UserImpl(String id, String name) {
        this(id, "", name, "");
        this.domainGroup = true;
    }

    public void setDomainGroup(boolean domainGroup) {
        this.domainGroup = domainGroup;
    }

    public void setSiteAdmin(boolean siteAdmin) {
        this.siteAdmin = siteAdmin;
    }

    public String getEmail() {
        return email;
    }

    public String getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getName() {
        return name;
    }

    public boolean isDomainGroup() {
        return domainGroup;
    }

    public boolean isSiteAdmin() {
        return siteAdmin;
    }

}
