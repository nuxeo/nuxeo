/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.connect.update;

import org.nuxeo.connect.update.impl.UpdateServiceImpl;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.RuntimeContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class PackageUpdateComponent extends DefaultComponent {

    protected UpdateServiceImpl svc;

    protected static RuntimeContext ctx;

    public static RuntimeContext getContext() {
        return ctx;
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        ctx = context.getRuntimeContext();
        svc = new UpdateServiceImpl();
        svc.initialize();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        svc.shutdown();
        svc = null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        return adapter == PackageUpdateService.class ? (T) svc : null;
    }

}
