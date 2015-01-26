/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.registry.context;

/**
 * Exception thrown when too much level of marshaller where loaded in cascade.
 *
 * @since 7.2
 */
public class MaxDepthReachedException extends Exception {

    private static final long serialVersionUID = 1L;

    public MaxDepthReachedException() {
        super();
    }

    public MaxDepthReachedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public MaxDepthReachedException(String message, Throwable cause) {
        throw new UnsupportedOperationException();
    }

    public MaxDepthReachedException(String message) {
        super(message);
    }

    public MaxDepthReachedException(Throwable cause) {
        super(cause);
    }

}
