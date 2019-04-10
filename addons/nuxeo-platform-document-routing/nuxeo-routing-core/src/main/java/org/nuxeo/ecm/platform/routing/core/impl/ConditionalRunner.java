/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.routing.core.impl;

import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteStepsContainer;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;

/**
 * Run the first step and then run the folder child in the position determined by the posOfChildStepToRunNext on this
 * container. It's done once the selected branch to be run is done .
 *
 * @since 5.5
 * @deprecated since 5.9.2 - Use only routes of type 'graph'
 */
@Deprecated
public class ConditionalRunner extends SerialRunner {

    @Override
    public void run(CoreSession session, DocumentRouteElement element) {
        String posOfchildToRun = null;
        try {
            posOfchildToRun = (String) element.getDocument().getPropertyValue(
                    DocumentRoutingConstants.STEP_TO_BE_EXECUTED_NEXT_PROPERTY_NAME);
        } catch (PropertyException e) {
        }

        List<DocumentRouteElement> children = getChildrenElement(session, element);
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
                    if (String.valueOf(children.indexOf(child)).equals(posOfchildToRun)) {
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
