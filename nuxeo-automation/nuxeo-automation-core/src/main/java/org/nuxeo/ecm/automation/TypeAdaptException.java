/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.automation;

/**
 * @author Anahide Tchertchian
 */
public class TypeAdaptException extends OperationException {

    private static final long serialVersionUID = 1L;

    public TypeAdaptException(String message, Throwable cause) {
        super(message, cause);
    }

    public TypeAdaptException(String message) {
        super(message);
    }

    public TypeAdaptException(Throwable cause) {
        super(cause);
    }

}
