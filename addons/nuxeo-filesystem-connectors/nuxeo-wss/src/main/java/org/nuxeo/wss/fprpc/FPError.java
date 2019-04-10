/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.wss.fprpc;

public class FPError {

    public static final String WriteError = "0x0002000C";

    public static final String CanNotRenameConflict = "0x00020019";

    public static final String FileAlreadyExists = "0x00090002";

    public static final String UrlDoesNotExists = "0x00090005";

    public static final String FolderUrlDoesNotExists = "0x00090007";

    public static final String FolderNameAlreadyExists = "0x0009000D";

    public static final String AlreadyLocked = "589838";// "0x0009000E";

    public static final String NotCheckedOut = "0x0009000F";

    public static final String UnknownMethod = "0x000E0002";

    public static final String AccessDenied = "0x001E0002";

}
