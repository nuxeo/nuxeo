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
 *     Nuxeo - initial API and implementation
 *
 * $Id: InvalidPredicateException.java 19155 2007-05-22 16:19:48Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.api.exceptions;

/**
 * A predicate has to be a resource.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class InvalidPredicateException extends RuntimeException {

    private static final long serialVersionUID = -961763618434457797L;

    public InvalidPredicateException() {
    }

    public InvalidPredicateException(String message) {
        super(message);
    }

    public InvalidPredicateException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPredicateException(Throwable cause) {
        super(cause);
    }

}
