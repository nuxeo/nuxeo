/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.signature.api.pki;

/**
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a> Assures naming
 *         consistency for keystore aliases
 */
public class Alias {
    private String userName;

    /**
     * Public constructor for the Alias. This is the only way to associate a
     * user name with an Alias object. The userName constitutes the first part
     * of an Alias' identity. The second part of the identity depends on an
     * AliasType provided as a parameter.
     *
     * @param userName
     */
    public Alias(String userName) {
        this.userName = userName;
    }

    /**
     * Provides the user name associated with the alias for generic checking of
     * alias groups (an alias group would share the name, not the id).
     *
     * @return
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Provides a unique identifier for an alias to be stored in a keystore.
     */
    public String getId(AliasType type) {
        return userName + type.toString();
    }

}
