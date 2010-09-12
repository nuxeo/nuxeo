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
package org.nuxeo.ecm.platform.routing.core.impl;

import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;

/**
 * @author arussel
 *
 */
public class DocumentRouteParallelStepsContainer extends
        DocumentRouteStepsContainerImpl {

    public DocumentRouteParallelStepsContainer(DocumentModel doc) {
        super(doc);
    }

    @Override
    public void run(CoreSession session) {
        List<DocumentRouteElement> children = getChildrenElement(session);
        if (children.isEmpty()) {
            setRunning(session);
            setDone(session);
            return;
        }
        if (!isRunning()) {
            setRunning(session);
            boolean someChildrenNotDone = false;
            for (DocumentRouteElement child : children) {
                child.run(session);
                if (!child.isDone()) {
                    someChildrenNotDone = true;
                }
            }
            if (!someChildrenNotDone) {
                setDone(session);
            }
            return;
        } else {
            boolean someChildrenNotDone = false;
            for (DocumentRouteElement child : children) {
                if (!child.isDone()) {
                    someChildrenNotDone = true;
                }
            }
            if (!someChildrenNotDone) {
                setDone(session);
            }
            return;
        }

    }
}
