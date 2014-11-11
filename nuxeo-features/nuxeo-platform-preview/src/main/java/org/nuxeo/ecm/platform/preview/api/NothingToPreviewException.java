/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.platform.preview.api;

/**
 * Preview exception.
 * Use this when there is nothing to preview and this is not an error.
 * ie when there is no Blob to preview (not when it cannot be found)
 *
 */
public class NothingToPreviewException extends PreviewException {

    private static final long serialVersionUID = 1L;

    public NothingToPreviewException(Throwable cause) {
        super(cause);
    }

    public NothingToPreviewException(String message, Throwable cause) {
        super(message, cause);
    }

    public NothingToPreviewException(String message) {
        super(message);
    }

}
