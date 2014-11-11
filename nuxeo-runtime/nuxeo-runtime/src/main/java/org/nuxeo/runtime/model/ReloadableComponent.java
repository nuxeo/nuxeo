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

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.runtime.service.TimestampedService;

/**
 * A component that expose a reload method usefull to completely reload the
 * component and preserving already registered extensions.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @deprecated since 5.6: services needing a reload should listen to runtime
 *             reload events instead. They can also implement the
 *             {@link TimestampedService} interface in case they should not
 *             need to be reloaded when event is received.
 */
@Deprecated
public class ReloadableComponent extends DefaultComponent implements Reloadable {

    protected List<Extension> extensions = new ArrayList<Extension>();

    @Override
    public void registerExtension(Extension extension) throws Exception {
        super.registerExtension(extension);
        extensions.add(extension);
    }

    @Override
    public void unregisterExtension(Extension extension) throws Exception {
        extensions.remove(extension);
        super.unregisterExtension(extension);
    }

    @Override
    public void reload(ComponentContext context) throws Exception {
        deactivate(context);
        activate(context);
        for (Extension xt : extensions) {
            super.registerExtension(xt);
        }
    }

    public List<Extension> getExtensions() {
        return extensions;
    }

}
