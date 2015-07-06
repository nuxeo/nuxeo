/*
 * (C) Copyright 2006-20012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.content;

import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Content diff exception.
 *
 * @author Antoine Taillefer
 * @since 5.6
 */
public class ContentDiffException extends NuxeoException {

    private static final long serialVersionUID = 1L;

    public ContentDiffException(Throwable cause) {
        super(cause);
    }

    public ContentDiffException(String message, Throwable cause) {
        super(message, cause);
    }

    public ContentDiffException(String message) {
        super(message);
    }
}
