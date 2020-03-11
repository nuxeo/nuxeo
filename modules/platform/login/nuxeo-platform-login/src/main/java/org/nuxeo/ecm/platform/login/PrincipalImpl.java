/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
     * Returns true if the object passed matches the principal represented in this implementation.
     *
     * @param another the Principal to compare with.
     * @return true if the Principal passed is the same as that encapsulated in this object, false otherwise
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
    @Override
    public String getName() {
        return user;
    }

}
