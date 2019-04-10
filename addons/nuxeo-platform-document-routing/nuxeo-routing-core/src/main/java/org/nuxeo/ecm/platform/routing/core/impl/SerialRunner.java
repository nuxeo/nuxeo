/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;

/**
 * Run all its children one after the other and is donen when the last children is done.
 *
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 * @deprecated since 5.9.2 - Use only routes of type 'graph'
 */
@Deprecated
public class SerialRunner extends AbstractRunner implements ElementRunner {

    @Override
    public void run(CoreSession session, DocumentRouteElement element) {
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
            if (!child.isDone()) {
                child.run(session);
                if (!child.isDone()) {
                    return;
                }
            }
        }
        // all child ran, we're done
        element.setDone(session);
    }
}
