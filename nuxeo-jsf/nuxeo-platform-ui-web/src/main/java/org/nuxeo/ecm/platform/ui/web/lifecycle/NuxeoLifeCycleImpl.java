/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.lifecycle;

import java.util.ArrayList;
import java.util.List;

import javax.faces.FacesException;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseListener;
import javax.faces.lifecycle.Lifecycle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Wrapper for a standard life cycle implementation that gets rid of duplicate
 * phase listeners.
 *
 * @see {@link NuxeoLifeCycleFactory}.
 *
 * @author Anahide Tchertchian
 */
public class NuxeoLifeCycleImpl extends Lifecycle {

    private static final Log log = LogFactory.getLog(NuxeoLifeCycleImpl.class);

    protected final Lifecycle original;

    public NuxeoLifeCycleImpl(Lifecycle original) {
        this.original = original;
    }

    /**
     * Checks for duplicate listeners using the instance class name.
     *
     * @return true if a listener with same class is already registered.
     */
    protected boolean checkDuplicateListener(PhaseListener listener) {
        if (listener != null) {
            List<String> existingClasses = new ArrayList<String>();
            for (PhaseListener existing : getPhaseListeners()) {
                existingClasses.add(existing.getClass().getName());
            }
            if (existingClasses.contains(listener.getClass().getName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addPhaseListener(PhaseListener listener) {
        if (checkDuplicateListener(listener)) {
            if (log.isTraceEnabled()) {
                log.trace("Duplicate life cycle listener detected: "
                        + listener.getClass().getName());
            }
        } else {
            original.addPhaseListener(listener);
        }
    }

    @Override
    public void execute(FacesContext context) throws FacesException {
        original.execute(context);
    }

    @Override
    public PhaseListener[] getPhaseListeners() {
        return original.getPhaseListeners();
    }

    @Override
    public void removePhaseListener(PhaseListener listener) {
        original.removePhaseListener(listener);
    }

    @Override
    public void render(FacesContext context) throws FacesException {
        original.render(context);
    }

}
