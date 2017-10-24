/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.model.exceptions;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * A query / form parameter was expected but not found or has invalid value.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class IllegalParameterException extends NuxeoException {

    private static final long serialVersionUID = 1L;

    public IllegalParameterException(String message) {
        super(message, SC_BAD_REQUEST);
    }

}
