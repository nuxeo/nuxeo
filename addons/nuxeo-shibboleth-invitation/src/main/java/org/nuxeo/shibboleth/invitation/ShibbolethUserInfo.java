/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */

package org.nuxeo.shibboleth.invitation;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;

/**
 * @since 7.4
 */
public class ShibbolethUserInfo extends UserIdentificationInfo {

    private static final long serialVersionUID = 6894397878763275157L;

    protected String firstName;

    protected String lastName;

    protected String company;

    protected Set<String> roles;

    protected String email;

    private ShibbolethUserInfo(String emailAsUserName, String password) {
        super(emailAsUserName, password);
    }

    public ShibbolethUserInfo(String username, String password, String firstName, String lastName, String company, String email) {
        super(username, password);

        if (username == null || StringUtils.isEmpty(username)) {
            throw new IllegalStateException("A valid username should always be provided");
        }

        this.firstName = firstName;
        this.lastName = lastName;
        this.company = company;
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getCompany() {
        return company;
    }

    public String getEmail() {
        return email;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

}