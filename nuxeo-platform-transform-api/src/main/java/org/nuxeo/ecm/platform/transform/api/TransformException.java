/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: TransformServiceDelegate.java 20875 2007-06-19 20:26:53Z sfermigier $
 */
package org.nuxeo.ecm.platform.transform.api;

import org.nuxeo.ecm.core.api.WrappedException;

/**
 * Transform service general exception.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 *
 */
public class TransformException extends Exception {

    private static final long serialVersionUID = 7572310562544004499L;

    public TransformException() {
    }

    public TransformException(String message, Throwable cause) {
        super(message, WrappedException.wrap(cause));
    }

    public TransformException(String message) {
        super(message);
    }

    public TransformException(Throwable cause) {
        super(WrappedException.wrap(cause));
    }
}
