/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.connect.update;

import java.io.IOException;

import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.connect.update.live.UpdateServiceImpl;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.RuntimeContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class PackageUpdateComponent extends DefaultComponent {

    protected UpdateServiceImpl svc;

    protected RuntimeContext ctx;

    @Override
    public void activate(ComponentContext context) {
        ctx = context.getRuntimeContext();
        try {
            svc = new UpdateServiceImpl();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            svc.initialize();
        } catch (PackageException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deactivate(ComponentContext context) {
        try {
            svc.shutdown();
        } catch (PackageException e) {
            throw new RuntimeException(e);
        }
        svc = null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        return adapter == PackageUpdateService.class ? (T) svc : null;
    }

}
