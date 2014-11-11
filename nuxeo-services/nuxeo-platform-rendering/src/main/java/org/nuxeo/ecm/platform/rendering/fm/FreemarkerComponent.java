/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.fm;

import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class FreemarkerComponent extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName(
            FreemarkerComponent.class.getName());

    @Override
    public void activate(ComponentContext context) throws Exception {
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
    }

    public FreemarkerEngine newEngine() {
        return new FreemarkerEngine();
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == FreemarkerComponent.class) {
            return adapter.cast(this);
        }
        return null;
    }

}
