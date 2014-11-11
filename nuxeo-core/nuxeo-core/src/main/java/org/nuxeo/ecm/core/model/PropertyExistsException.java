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

package org.nuxeo.ecm.core.model;

import org.nuxeo.ecm.core.api.DocumentException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class PropertyExistsException extends DocumentException {

    private static final long serialVersionUID = -2744378981592186415L;

    public PropertyExistsException() {
        super("The property already exists");
    }

    public PropertyExistsException(String path) {
        super("The property at '" + path + "' already exists");
    }

    public PropertyExistsException(String path, Throwable cause) {
        super("The property at '" + path + "' already exists", cause);
    }

}
