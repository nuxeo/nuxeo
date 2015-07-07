/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *     Anahide Tchertchian
 */

package org.nuxeo.theme.styling.service.palettes;

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
