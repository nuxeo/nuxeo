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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.routing.core.impl;

import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteStepsContainer;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;

/**
 * Run the first step and then run the folder child in the position determined
 * by the posOfChildStepToRunNext on this container. It's done once the selected
 * branch to be run is done .
 * 
 * @since 5.5
 * 
 */
public class ConditionalRunner extends SerialRunner {

    @Override
    public void run(CoreSession session, DocumentRouteElement element) {
        String posOfchildToRun = null;
        try {
            posOfchildToRun = (String) element.getDocument().getPropertyValue(
                    DocumentRoutingConstants.STEP_TO_BE_EXECUTED_NEXT_PROPERTY_NAME);
        } catch (Exception e) {
        }

        List<DocumentRouteElement> children = getChildrenElement(session,
                element);
        if (!element.isRunning()) {
            element.setRunning(session);
        }
        if (children.isEmpty()) {
            element.setDone(session);
            return;
        }
        // run all the child unless there is a wait state
        for (DocumentRouteElement child : children) {
            if (!child.isDone() && !child.isCanceled()) {
                if (!(child instanceof DocumentRouteStepsContainer)) {
                    // run the simple step
                    child.run(session);
                    if (!child.isDone()) {
                        return;
                    }
                } else if (child instanceof DocumentRouteStepsContainer) {
                    // run only the child that was selected to be run by the
                    // previous step
                    if (String.valueOf(children.indexOf(child)).equals(
                                    posOfchildToRun)) {
                        child.run(session);
                        if (!child.isDone()) {
                            return;
                        }
                    } else {
                        // cancel the branch that won;t be run
                        if (!child.isCanceled()) {
                            child.cancel(session);
                        }
                    }
                }
            }
        }
        // all child ran, we're done
        element.setDone(session);
    }
}
