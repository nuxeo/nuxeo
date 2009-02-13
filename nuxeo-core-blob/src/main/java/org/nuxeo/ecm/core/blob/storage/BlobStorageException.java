/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.blob.storage;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class BlobStorageException extends Exception {

    private static final long serialVersionUID = 4621055332439939600L;

    public BlobStorageException() {
        // TODO Auto-generated constructor stub
    }

    public BlobStorageException(String message) {
        super (message);
    }

    public BlobStorageException(String message, Throwable cause) {
        super (message, cause);
    }

    public BlobStorageException(Throwable cause) {
        super (cause);
    }


}
