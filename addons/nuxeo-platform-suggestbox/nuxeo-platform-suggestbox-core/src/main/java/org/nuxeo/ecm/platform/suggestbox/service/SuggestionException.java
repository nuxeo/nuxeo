/*
 * (C) Copyright 2010-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Olivier Grisel
 */
package org.nuxeo.ecm.platform.suggestbox.service;

/**
 * Exception raised when a suggester cannot perform it's suggestion due to
 * inconsistent configuration or problem when calling a backend service: in that
 * case the backend service exception should be wrapped as the cause.
 */
public class SuggestionException extends Exception {

    private static final long serialVersionUID = 1L;

    public SuggestionException(String msg) {
        super(msg);
    }

    public SuggestionException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public SuggestionException(Throwable cause) {
        super(cause);
    }
}
