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
 * List of required X500 Principal fields required for certificate generation
 *
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 */
public enum CNField {
    /**
     * Country name
     */
    C,
    /**
     * Organization name
     */
    O,
    /**
     * Organizational Unit name
     */
    OU,
    // TODO to be added
    /**
     * Localization
     */
    // L,
    /**
     * Common Name
     */
    CN,
    /**
     * Email
     */
    Email,
    /**
     * UserID
     */
    UserID
}
