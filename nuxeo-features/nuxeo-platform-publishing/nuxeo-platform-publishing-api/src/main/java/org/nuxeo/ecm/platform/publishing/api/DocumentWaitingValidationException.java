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
 *     arussel
 */
package org.nuxeo.ecm.platform.publishing.api;

/**
 * @author arussel
 *
 */
public class DocumentWaitingValidationException extends PublishingException {

    private static final long serialVersionUID = 1L;

    public DocumentWaitingValidationException() {
    }

    public DocumentWaitingValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DocumentWaitingValidationException(String message) {
        super(message);
    }

    public DocumentWaitingValidationException(Throwable cause) {
        super(cause);
    }

}
