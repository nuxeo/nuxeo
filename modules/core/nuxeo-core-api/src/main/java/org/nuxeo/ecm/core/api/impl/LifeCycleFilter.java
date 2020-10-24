/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.impl;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Filter;

/**
 * A filter based on the document's life cycle.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class LifeCycleFilter implements Filter {

    private final List<String> accepted;

    private final List<String> excluded;

    /**
     * Generic constructor.
     * <p>
     * To be accepted, the document must have its lifecycle state in the {@code required} list and the {@code excluded}
     * list must not contain its lifecycle state.
     *
     * @param accepted the list of accepted lifecycle states
     * @param excluded the list of excluded lifecycle states
     */
    public LifeCycleFilter(List<String> accepted, List<String> excluded) {
        this.accepted = accepted;
        this.excluded = excluded;
    }

    /**
     * Convenient constructor to filter on a lifecycle state.
     *
     * @param lifeCycle the lifecycle to filter on
     * @param isRequired if {@code true} accepted documents must have this lifecycle state, if {@code false} accepted
     *            documents must not have this lifecycle state.
     */
    public LifeCycleFilter(String lifeCycle, boolean isRequired) {
        if (isRequired) {
            accepted = new ArrayList<>();
            excluded = null;
            accepted.add(lifeCycle);
        } else {
            excluded = new ArrayList<>();
            accepted = null;
            excluded.add(lifeCycle);
        }
    }

    @Override
    public boolean accept(DocumentModel docModel) {
        String lifeCycleState = docModel.getCurrentLifeCycleState();
        if (excluded != null) {
            if (excluded.contains(lifeCycleState)) {
                return false;
            }
        }
        if (accepted != null) {
            if (!accepted.contains(lifeCycleState)) {
                return false;
            }
        }
        return true;
    }

}
