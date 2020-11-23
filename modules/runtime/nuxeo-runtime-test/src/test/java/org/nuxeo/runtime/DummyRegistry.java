/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.runtime;

import java.util.Optional;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.AbstractRegistry;
import org.nuxeo.common.xmap.registry.Registry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ExtensionPoint;
import org.w3c.dom.Element;

/**
 * @since TODO
 */
public class DummyRegistry extends AbstractRegistry {

    public Optional<Registry> getTargetRegistry() {
        return Framework.getRuntime()
                        .getComponentManager()
                        .getRegistrationInfo(ComponentWithXPoint.NAME)
                        .getExtensionPoint(ComponentWithXPoint.XP)
                        .map(ExtensionPoint::getRegistry);
    }

    @Override
    public void register(Context ctx, XAnnotatedObject xObject, Element element, String flag) {
        getTargetRegistry().ifPresentOrElse(reg -> {
            reg.register(ctx, xObject, element, flag);
        }, () -> new RuntimeException());
    }

    @Override
    protected void register(Context ctx, XAnnotatedObject xObject, Element element) {
        // NOOP
    }

    @Override
    public void unregister(String flag) {
        getTargetRegistry().ifPresentOrElse(reg -> {
            reg.unregister(flag);
        }, () -> new RuntimeException());
    }

}
