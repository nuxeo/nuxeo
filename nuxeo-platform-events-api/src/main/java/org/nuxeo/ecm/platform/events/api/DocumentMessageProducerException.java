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
 *
 * $Id: DocumentMessageProducerException.java 1277 2006-07-22 00:44:40Z janguenot $
 */

package org.nuxeo.ecm.platform.events.api;

import org.nuxeo.ecm.core.CoreException;

/**
 * Exception thrown by JMSDocumentMessageProducer.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class DocumentMessageProducerException extends CoreException {

    private static final long serialVersionUID = 6885905202244173862L;

    public DocumentMessageProducerException() {
    }

    public DocumentMessageProducerException(String message) {
        super(message);
    }

    public DocumentMessageProducerException(String message, Throwable cause) {
        super(message, cause);
    }

    public DocumentMessageProducerException(Throwable cause) {
        super(cause);
    }

}
