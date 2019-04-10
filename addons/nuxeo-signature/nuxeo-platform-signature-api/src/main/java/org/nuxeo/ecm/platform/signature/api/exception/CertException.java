/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *    Wojciech Sulejman
 */
package org.nuxeo.ecm.platform.signature.api.exception;

/**
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 *
 */

/**
 * An exception indicating certificate or key generation related problems
 *
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 */
public class CertException extends SignException {
    private static final long serialVersionUID = 1L;

    public CertException(String message, Throwable e) {
        super(message, e);
    }

    public CertException(String message) {
        super(message);
    }

    public CertException(Throwable e) {
        super(e);
    }
}