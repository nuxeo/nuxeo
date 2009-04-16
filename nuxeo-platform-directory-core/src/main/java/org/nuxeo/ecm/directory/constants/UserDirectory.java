/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.directory.constants;

/**
 * @deprecated Use UserManager configuration instead.
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
@Deprecated
public class UserDirectory {

    public static final String DIRECTORY_NAME = "userDirectory";

    public static final String AUTHENTICATION_DIRECTORY_NAME = "userAuthentication";

    public static final String FIRSTNAME_COLUMN = "firstName";

    public static final String LASTNAME_COLUMN = "lastName";

    public static final String COMPANY_COLUMN = "company";

    public static final String EMAIL_COLUMN = "email";

    // Utility class.
    private UserDirectory() {
    }

}
