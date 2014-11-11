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
 * $Id: ExportDocumentException.java 29029 2008-01-14 18:38:14Z ldoguin $
 */

package org.nuxeo.ecm.core.io.exceptions;

/**
 * Import document exception.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class ExportDocumentException extends RuntimeException {

    private static final long serialVersionUID = 2045308733731717902L;

    public ExportDocumentException() {
    }

    public ExportDocumentException(String message) {
        super(message);
    }

    public ExportDocumentException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExportDocumentException(Throwable cause) {
        super(cause);
    }
}
