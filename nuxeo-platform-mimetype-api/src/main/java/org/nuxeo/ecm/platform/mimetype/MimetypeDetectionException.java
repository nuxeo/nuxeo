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
 * $Id: NXMimeType.java 16046 2007-04-12 14:34:58Z fguillaume $
 */
package org.nuxeo.ecm.platform.mimetype;

/**
 * Exception raised when an unexpected exception occur during mimetype sniffing.
 *
 * @author ogrisel@nuxeo.com
 */
public class MimetypeDetectionException extends Exception {

    private static final long serialVersionUID = 1L;

    public MimetypeDetectionException() {
    }

    public MimetypeDetectionException(String message) {
        super(message);
    }

    public MimetypeDetectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public MimetypeDetectionException(Throwable cause) {
        super(cause);
    }

}
