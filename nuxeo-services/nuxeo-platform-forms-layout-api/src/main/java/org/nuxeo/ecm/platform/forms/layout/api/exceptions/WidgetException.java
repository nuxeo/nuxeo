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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: WidgetException.java 26053 2007-10-16 01:45:43Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.api.exceptions;

/**
 * Widget exception
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class WidgetException extends RuntimeException {

    private static final long serialVersionUID = -961763618434457797L;

    public WidgetException() {
    }

    public WidgetException(String message) {
        super(message);
    }

    public WidgetException(String message, Throwable cause) {
        super(message, cause);
    }

    public WidgetException(Throwable cause) {
        super(cause);
    }
}
