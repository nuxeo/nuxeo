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
 * $Id: NoExtensionFoundException.java 16046 2007-04-12 14:34:58Z fguillaume $
 */

package org.nuxeo.ecm.platform.transform.exceptions;

import org.nuxeo.ecm.core.api.WrappedException;

/**
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class NoExtensionFoundException extends Exception {

    private static final long serialVersionUID = -961763618434457798L;

    public NoExtensionFoundException() {
    }

    public NoExtensionFoundException(String message) {
        super(message);
    }

    public NoExtensionFoundException(String message, Throwable cause) {
        super(message, WrappedException.wrap(cause));
    }

    public NoExtensionFoundException(Throwable cause) {
        super(WrappedException.wrap(cause));
    }

}
