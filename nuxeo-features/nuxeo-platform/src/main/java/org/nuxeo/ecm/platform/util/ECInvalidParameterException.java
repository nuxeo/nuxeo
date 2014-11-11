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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.util;

/**
 * TODO: move to another package.
 * Invalid parameters have been passed to NXPlatform methods.
 *
 * @author Razvan Caraghin
 *
 */
public class ECInvalidParameterException extends Exception {
    private static final long serialVersionUID = 650130019430248750L;

    public ECInvalidParameterException() {
    }

    /**
     * @param message
     */
    public ECInvalidParameterException(String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public ECInvalidParameterException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause
     */
    public ECInvalidParameterException(Throwable cause) {
        super(cause);
    }

}
