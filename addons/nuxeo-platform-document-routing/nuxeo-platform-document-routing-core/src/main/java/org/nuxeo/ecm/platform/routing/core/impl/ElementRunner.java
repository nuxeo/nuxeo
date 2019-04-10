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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.routing.core.impl;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;

/**
 * Runner responsible to run or undo an element of a route.
 *
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 *
 */
public interface ElementRunner {
    /**
     * Run this element. If an exception is thrown while doing, it cancels the
     * route.
     */
    void run(CoreSession session, DocumentRouteElement element);

    /**
     * Run the undo chain on this element. If this element is not a step, then
     * throw an exception.
     */
    void undo(CoreSession session, DocumentRouteElement element);

    /**
     * Cancel this element.
     *
     * @see DocumentRoute#cancel(CoreSession)
     */
    void cancel(CoreSession session, DocumentRouteElement element);
}
