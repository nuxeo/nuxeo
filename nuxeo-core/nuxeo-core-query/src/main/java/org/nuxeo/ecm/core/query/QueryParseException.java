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

package org.nuxeo.ecm.core.query;

import org.nuxeo.ecm.core.CoreRuntimeException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class QueryParseException extends CoreRuntimeException {

    private static final long serialVersionUID = -3557588796412733083L;

    public QueryParseException() {
    }

    public QueryParseException(String message) {
        super(message);
    }

    public QueryParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public QueryParseException(Throwable cause) {
        super(cause);
    }

}
