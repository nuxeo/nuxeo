/*
 * (C) Copyright 2006-2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.usermapper.test.dummy;

/**
 * 
 * @author tiry
 *
 */
public class DummyUser {

    String login;

    Name name;

    public DummyUser(String login, String fname, String lname) {
        this.login = login;
        name = new Name();
        name.firstName = fname;
        name.lastName = lname;
    }

    public class Name {

        String firstName;

        String lastName;

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

    }

    public String getLogin() {
        return login;
    }

    public Name getName() {
        return name;
    }

}
