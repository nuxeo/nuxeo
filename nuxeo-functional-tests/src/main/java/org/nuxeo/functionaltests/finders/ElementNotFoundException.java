package org.nuxeo.functionaltests.finders;
/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Sun Seng David TAN <stan@nuxeo.com>
 */


/**
 * Exception thrown when an element can't be found after time out
 *
 * @author Sun Seng David TAN <stan@nuxeo.com>
 *
 */
public class ElementNotFoundException extends Exception {

    private static final long serialVersionUID = 1L;

    public ElementNotFoundException(String message, Throwable lastException) {
        super(message, lastException);
    }

}
