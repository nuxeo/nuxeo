/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
     */
    public AliasWrapper(String userName) {
        this.userName = userName;
    }

    /**
     * Provides the user name associated with the alias for generic checking of alias groups (an alias group would share
     * the name, not the id).
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
