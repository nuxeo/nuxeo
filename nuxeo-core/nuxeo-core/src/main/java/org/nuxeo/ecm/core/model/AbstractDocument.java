/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.model;

import java.util.Collection;
import java.util.Collections;

import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.lifecycle.LifeCycle;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.core.lifecycle.LifeCycleService;

/**
 * Abstract document that implements code independent on the model
 * implementation.
 * <p>
 * The Document implementors MUST extend this class and not directly implement
 * the Document interface.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public abstract class AbstractDocument implements Document {

    @Override
    public boolean followTransition(String transition)
            throws LifeCycleException {
        LifeCycleService service = NXCore.getLifeCycleService();
        service.followTransition(this, transition);
        // TODO handle this case.
        return true;
    }

    @Override
    public Collection<String> getAllowedStateTransitions()
            throws LifeCycleException {
        LifeCycleService service = NXCore.getLifeCycleService();
        LifeCycle lifeCycle = service.getLifeCycleFor(this);
        if (lifeCycle != null) {
            return lifeCycle.getAllowedStateTransitionsFrom(getLifeCycleState());
        } else {
            return Collections.emptyList();
        }
    }

}
