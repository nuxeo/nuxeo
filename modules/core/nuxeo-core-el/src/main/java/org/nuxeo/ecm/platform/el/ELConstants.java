/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

package org.nuxeo.ecm.platform.el;

/**
 * Variables for EL expression
 *
 * @since 9.1
 */
public class ELConstants {

    private ELConstants() {
        // Empty
    }

    /**
     * Principal variable
     */
    public static final String PRINCIPAL = "principal";

    /**
     * Current user variable
     */
    public static final String CURRENT_USER = "currentUser";

    /**
     * Current document variable
     */
    public static final String DOCUMENT = "document";

    /**
     * Previous document variable
     */
    public static final String PREVIOUS_DOCUMENT = "previousDocument";

    /**
     * Current document variable
     */
    public static final String CURRENT_DOCUMENT = "currentDocument";

}
