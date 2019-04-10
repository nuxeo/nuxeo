/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *    Wojciech Sulejman
 */
package org.nuxeo.ecm.platform.signature.api.user;

/**
 * Represents a keystore alias.
 * <p>
 * Provides methods for binding a keystore alias name with userID and alias type.
 *
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 */
public class AliasWrapper {
    private String userName;

    /**
     * Public constructor for the AliasWrapper. Used to associate a user name with an AliasWrapper object. The userName
     * constitutes the first part of the AliasWrapper's identity. The second part of the identity is based on an
     * AliasType provided as a parameter. E.g., for a user identified by string "jdoe" and using a type "cert", the
     * produced alias string would be "jdoecert".
     *
     * @param userName
     */
    public AliasWrapper(String userName) {
        this.userName = userName;
    }

    /**
     * Provides the user name associated with the alias for generic checking of alias groups (an alias group would share
     * the name, not the id).
     *
     * @return
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Returns a unique identifier for the keystore alias.
     */
    public String getId(AliasType type) {
        return userName + type.toString();
    }
}
