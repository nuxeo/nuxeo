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
 *      Stephane Lacoin (Nuxeo EP Software Engineer)
 */
package org.nuxeo.runtime.management;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 * 
 */
public class ManagementRuntimeException extends RuntimeException {

    private static final long serialVersionUID = -8772340021060960325L;

    public static ManagementRuntimeException wrap(String message,
            Exception cause) {
        return new ManagementRuntimeException(message, cause);
    }

    public static ManagementRuntimeException wrap(Exception cause) {
        return new ManagementRuntimeException(cause);
    }

    public ManagementRuntimeException() {
    }

    public ManagementRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ManagementRuntimeException(String message) {
        super(message);
    }

    public ManagementRuntimeException(Throwable cause) {
        super(cause);
    }
}
