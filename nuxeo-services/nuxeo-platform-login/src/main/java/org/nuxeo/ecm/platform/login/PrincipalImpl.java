/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.login;

import java.security.*;

/**
 * This class implements the principal interface.
 *
 * @author Satish Dharmaraj
 */
public class PrincipalImpl implements Principal {

    private String user;

    /**
     * Construct a principal from a string user name.
     *
     * @param user The string form of the principal name.
     */
    public PrincipalImpl(String user) {
        this.user = user;
    }

    /**
     * Returns true if the object passed matches the principal represented in
     * this implementation.
     *
     * @param another the Principal to compare with.
     * @return true if the Principal passed is the same as that encapsulated in
     *         this object, false otherwise
     */
    @Override
    public boolean equals(Object another) {
        if (another instanceof PrincipalImpl) {
            PrincipalImpl p = (PrincipalImpl) another;
            return user.equals(p.toString());
        } else {
            return false;
        }
    }

    /**
     * Prints a stringified version of the principal.
     */
    @Override
    public String toString() {
        return user;
    }

    /**
     * return a hashcode for the principal.
     */
    @Override
    public int hashCode() {
        return user.hashCode();
    }

    /**
     * return the name of the principal.
     */
    public String getName() {
        return user;
    }

}
