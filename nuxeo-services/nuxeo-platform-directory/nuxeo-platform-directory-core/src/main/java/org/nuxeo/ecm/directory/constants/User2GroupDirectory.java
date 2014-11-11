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
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
@Deprecated
public class User2GroupDirectory {
    public static final String DIRECTORY_NAME = "user2groupDirectory";
    public static final String USERID_COLUMN = "userId";
    public static final String GROUPID_COLUMN = "groupId";

    // Utility class.
    private User2GroupDirectory() {
    }

}
