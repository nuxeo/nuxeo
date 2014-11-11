/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.impl;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Filter;

/**
 * A filter based on the document's life cycle.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class LifeCycleFilter implements Filter {

    private static final long serialVersionUID = -6222667096842773182L;

    private final List<String> accepted;

    private final List<String> excluded;

    /**
     * Generic constructor.
     * <p>
     * To be accepted, the document must have its lifecycle state in the {@code
     * required} list and the {@code excluded} list must not contain its
     * lifecycle state.
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
     * @param isRequired if {@code true} accepted documents must have this
     *            lifecycle state, if {@code false} accepted documents must not
     *            have this lifecycle state.
     */
    public LifeCycleFilter(String lifeCycle, boolean isRequired) {
        if (isRequired) {
            accepted = new ArrayList<String>();
            excluded = null;
            accepted.add(lifeCycle);
        } else {
            excluded = new ArrayList<String>();
            accepted = null;
            excluded.add(lifeCycle);
        }
    }

    @Override
    public boolean accept(DocumentModel docModel) {
        try {
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
        } catch (ClientException e) {
            // refuse the document if the lifecycle state cannot be retrieved
            return false;
        }
    }

}
