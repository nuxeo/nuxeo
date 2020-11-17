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
package org.nuxeo.ecm.platform.routing.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteStep;
import org.nuxeo.ecm.platform.routing.api.helper.ActionableValidator;

/**
 * A Test Helper class that simulate persistence of Step information. This persistence is transient to the JVM.
 *
 * @deprecated since 5.9.2 - Use only routes of type 'graph'
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 */
@Deprecated
public class WaitingStepRuntimePersister {
    protected static final List<String> runningSteps = new ArrayList<>();

    protected static final List<String> doneSteps = new ArrayList<>();

    static public void addStepId(String id) {
        if (runningSteps.contains(id)) {
            throw new RuntimeException("Asking twice to wait on the same step.");
        }
        runningSteps.add(id);
    }

    static public List<String> getRunningStepIds() {
        return runningSteps;
    }

    static public DocumentRouteStep getStep(String id, CoreSession session) {
        return session.getDocument(new IdRef(id)).getAdapter(DocumentRouteStep.class);
    }

    static public List<String> getDoneStepIds() {
        return doneSteps;
    }

    static public void resumeStep(final String id, CoreSession session) {
        if (!runningSteps.contains(id)) {
            throw new RuntimeException("Asking to resume a non peristed step.");
        }
        ActionableValidator validator = new ActionableValidator(new SimpleActionableObject(id), session);
        validator.validate();
        runningSteps.remove(id);
        doneSteps.add(id);
    }

    static public void resumeDecisionalStep(final String id, CoreSession session, String nextStepPos) {
        if (!runningSteps.contains(id)) {
            throw new RuntimeException("Asking to resume a non peristed step.");
        }

        Map<String, Serializable> properties = new HashMap<>();
        properties.put("nextStepPos", nextStepPos);
        ActionableValidator validator = new ActionableValidator(new SimpleDecisionalActionableObject(id), session,
                properties);
        validator.validate();
        runningSteps.remove(id);
        doneSteps.add(id);
    }

    public static void removeStepId(String stepId) {
        runningSteps.remove(stepId);
        doneSteps.remove(stepId);
    }
}
