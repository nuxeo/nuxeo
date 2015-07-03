/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.query;

import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class QueryParseException extends NuxeoException {

    private static final long serialVersionUID = 1L;

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
