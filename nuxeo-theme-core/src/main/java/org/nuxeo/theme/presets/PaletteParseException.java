/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.presets;

public class PaletteParseException extends Exception {

    private static final long serialVersionUID = 1L;

    PaletteParseException() {
    }

    PaletteParseException(String message) {
        super(message);
    }

    PaletteParseException(String message, Throwable cause) {
        super(message, cause);
    }

    PaletteParseException(Throwable cause) {
        super(cause);
    }
}
