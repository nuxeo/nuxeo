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
public class ManagementException extends Exception {

    private static final long serialVersionUID = -8772340021060960325L;

    public static ManagementException wrap(String message,
            Exception cause) {
        return new ManagementException(message, cause);
    }

    public static ManagementException wrap(Exception cause) {
        return new ManagementException(cause);
    }

    public ManagementException() {
    }

    public ManagementException(String message, Throwable cause) {
        super(message, cause);
    }

    public ManagementException(String message) {
        super(message);
    }

    public ManagementException(Throwable cause) {
        super(cause);
    }
}
