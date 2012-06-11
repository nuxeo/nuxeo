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
 */
package org.nuxeo.runtime.model;

/**
 * A reloadable component.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @deprecated since 5.6: services needing a reload should listen to runtime
 *             reload events instead
 */
@Deprecated
public interface Reloadable {

    /**
     * Reload the component registries and services.
     *
     * @param context
     * @throws Exception
     */
    void reload(ComponentContext context) throws Exception;

}
