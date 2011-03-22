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
 */
package org.nuxeo.ecm.core.convert.api;

import org.nuxeo.ecm.core.api.ClientException;

/**
 * Base exception raised by the {@link ConversionService}.
 *
 * @author tiry
 */
public class ConversionException extends ClientException {

    private static final long serialVersionUID = 1L;

    public ConversionException(String message, Exception e) {
        super(message, e);
    }

    public ConversionException(String message) {
        super(message);
    }

    public ConversionException(String message, Throwable cause) {
        super(message, cause);
    }

}
