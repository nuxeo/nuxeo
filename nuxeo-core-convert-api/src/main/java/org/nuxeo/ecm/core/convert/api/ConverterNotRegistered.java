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

package org.nuxeo.ecm.core.convert.api;

/**
 * Exception thrown when selected converter is not registered.
 *
 * @author tiry
 */
public class ConverterNotRegistered extends ConversionException {

    private static final long serialVersionUID = 1L;

    public ConverterNotRegistered(String message) {
        super("Converter " + message + " is not registered");
    }

}
