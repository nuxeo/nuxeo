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
public class PropertyTypeException extends DocumentException {

    private static final long serialVersionUID = -6262464305550863643L;

    public PropertyTypeException() {
        super("The property doesn't exists");
    }

    public PropertyTypeException(String path) {
        super("Invalid property type '" + path);
    }

    public PropertyTypeException(String path, Throwable cause) {
        super("Invalid property type '" + path, cause);
    }

}
