/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.routing.api;

import org.nuxeo.ecm.core.api.CoreSession;

/**
 * A Step. The element of the route that will process the documents.
 *
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 *
 */
public interface DocumentRouteStep extends DocumentRouteElement {

    /**
     * Undo this step. This operation run the undo operation on this element.
     */
    DocumentRouteStep undo(CoreSession session);
}
