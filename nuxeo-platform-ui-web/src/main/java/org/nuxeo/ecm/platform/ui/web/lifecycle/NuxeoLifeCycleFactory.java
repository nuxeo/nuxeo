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

import java.util.HashMap;

import javax.faces.FacesException;
import javax.faces.lifecycle.Lifecycle;

import com.sun.faces.lifecycle.LifecycleFactoryImpl;

/**
 * This life cycle factory is used to override the default behaviour when
 * registering phase listeners.
 * <p>
 * When using jsf 1.2.0_09, all configs from both nuxeo.ear and
 * server/default/tmp are registered twice. Due to changes in configuration,
 * some phase listeners are registered twice too, for instance
 * SeamPhaseListener.
 *
 * @author Anahide Tchertchian
 */
public class NuxeoLifeCycleFactory extends LifecycleFactoryImpl {

    protected HashMap<String, Lifecycle> lifecycleMap = null;

    public NuxeoLifeCycleFactory() {
        lifecycleMap = new HashMap<String, Lifecycle>();
    }

    /**
     * Exposes wrapper of the actual life cycle.
     */
    @Override
    public Lifecycle getLifecycle(String lifecycleId) throws FacesException {
        if (!lifecycleMap.containsKey(lifecycleId)) {
            // try to add it
            Lifecycle original = super.getLifecycle(lifecycleId);
            if (original != null) {
                lifecycleMap.put(lifecycleId, new NuxeoLifeCycleImpl(original));
            }
        }
        return lifecycleMap.get(lifecycleId);
    }

}
