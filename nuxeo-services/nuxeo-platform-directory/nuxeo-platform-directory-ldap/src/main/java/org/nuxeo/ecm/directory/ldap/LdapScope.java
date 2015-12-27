/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.directory.ldap;

import javax.naming.directory.SearchControls;

/**
 * Helper for translating ldap search scope from string to integer
 *
 * @author Anahide Tchertchian
 */
public class LdapScope {

    /**
     * Returns the associated integer for scope, comparing lower case. Returns null if not one of "object", "onelevel"
     * or "subtree".
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
