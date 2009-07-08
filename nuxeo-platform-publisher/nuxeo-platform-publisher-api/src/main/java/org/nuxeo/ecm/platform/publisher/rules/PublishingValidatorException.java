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
 *     <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 * $Id: PublishingValidatorException.java 26321 2007-10-22 15:27:12Z janguenot $
 */

package org.nuxeo.ecm.platform.publisher.rules;

import org.nuxeo.ecm.core.api.ClientException;

/**
 * Publishing validator related exception.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class PublishingValidatorException extends ClientException {

    private static final long serialVersionUID = 1L;

    public PublishingValidatorException() {
    }

    public PublishingValidatorException(String message) {
        super(message);
    }

    public PublishingValidatorException(String message, Throwable cause) {
        super(message, cause);
    }

    public PublishingValidatorException(Throwable cause) {
        super(cause);
    }

}
