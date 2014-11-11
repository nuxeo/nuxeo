/*
 * (C) Copyright 2006-2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     tmartins
 * 
 */

package org.nuxeo.ecm.directory;

/**
 * Specific exception thrown when an entry is not found
 * 
 * @since 5.7
 * 
 * @author Thierry Martins <tm@nuxeo.com>
 * 
 */
public class DirectoryEntryException extends DirectoryException {

    private static final long serialVersionUID = 1L;

    public DirectoryEntryException() {
    }

    public DirectoryEntryException(String message, Throwable th) {
        super(message, th);
    }

    public DirectoryEntryException(String message) {
        super(message);
    }

    public DirectoryEntryException(Throwable th) {
        super(th);
    }

}
