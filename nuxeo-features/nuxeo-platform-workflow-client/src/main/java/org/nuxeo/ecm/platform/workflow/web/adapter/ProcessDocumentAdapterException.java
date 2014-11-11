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
 * $Id: ProcessDocumentAdapterException.java 19216 2007-05-23 10:35:15Z janguenot $
 */

package org.nuxeo.ecm.platform.workflow.web.adapter;

/**
 * Process document adapter related exception.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class ProcessDocumentAdapterException extends Exception {

    private static final long serialVersionUID = -961763618434457798L;

    public ProcessDocumentAdapterException() {

    }

    public ProcessDocumentAdapterException(String message) {
        super(message);
    }

    public ProcessDocumentAdapterException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProcessDocumentAdapterException(Throwable cause) {
        super(cause);
    }

}
