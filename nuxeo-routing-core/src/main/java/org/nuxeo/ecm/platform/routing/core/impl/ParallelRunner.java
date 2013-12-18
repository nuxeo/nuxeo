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

import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;

/**
 * Run all of its children simultaneous and is done when all the children are
 * done.
 *
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 * @deprecated since 5.9.2 - Use only routes of type 'graph'
 *
 */
@Deprecated
public class ParallelRunner extends AbstractRunner implements ElementRunner {

    @Override
    public void run(CoreSession session, DocumentRouteElement element) {
        List<DocumentRouteElement> children = getChildrenElement(session,
                element);
        if (children.isEmpty()) {
            element.setRunning(session);
            element.setDone(session);
            return;
        }
        if (!element.isRunning()) {
            element.setRunning(session);
            boolean someChildrenNotDone = false;
            for (DocumentRouteElement child : children) {
                child.run(session);
                if (!child.isDone()) {
                    someChildrenNotDone = true;
                }
            }
            if (!someChildrenNotDone) {
                element.setDone(session);
            }
            return;
        } else {
            boolean someChildrenNotDone = false;
            for (DocumentRouteElement child : children) {
                if (!child.isDone()) {
                    child.run(session);
                    if (!child.isDone()) {
                        someChildrenNotDone = true;
                    }
                }
            }
            if (!someChildrenNotDone) {
                element.setDone(session);
            }
        }
    }
}
