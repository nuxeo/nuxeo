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
 * $Id$
 */

package org.nuxeo.ecm.core.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author bstefanescu
 *
 */
public class ClientException extends Exception {

    private static final long serialVersionUID = 829907884555472415L;

    private static final Log log = LogFactory.getLog(ClientException.class);

    public ClientException() {
    }

    public ClientException(String message) {
        super(message);
    }

    public ClientException(String message, ClientException cause) {
        super(message, cause);
    }

    public ClientException(String message, Throwable cause) {
        super(message, WrappedException.wrap(cause));
    }

    public ClientException(Throwable cause) {
        super(WrappedException.wrap(cause));
    }

    public ClientException(ClientException cause) {
        super(cause);
    }

}
