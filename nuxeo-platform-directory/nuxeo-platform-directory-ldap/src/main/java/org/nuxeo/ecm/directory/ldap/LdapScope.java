/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.directory.ldap;

import javax.naming.directory.SearchControls;

/**
 * Helper for translating ldap search scope from string to integer
 *
 * @author Anahide Tchertchian
 *
 */
public class LdapScope {

    /**
     * Returns the associated integer for scope, comparing lower case. Returns
     * null if not one of "object", "onelevel" or "subtree".
     */
    public static Integer getIntegerScope(String scopeString) {
        if (scopeString != null) {
            scopeString = scopeString.toLowerCase();
            if ("object".equals(scopeString)) {
                return Integer.valueOf(SearchControls.OBJECT_SCOPE);
            } else if ("onelevel".equals(scopeString)) {
                return Integer.valueOf(SearchControls.ONELEVEL_SCOPE);
            } else if ("subtree".equals(scopeString)) {
                return SearchControls.SUBTREE_SCOPE;
            }
        }
        return null;
    }

}
