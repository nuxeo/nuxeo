/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.webengine.jaxrs.scan;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.nuxeo.ecm.webengine.jaxrs.ApplicationFactory;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DynamicApplicationFactory implements ApplicationFactory {

    @Override
    public Application getApplication(Bundle bundle, Map<String, String> args) throws ReflectiveOperationException,
            IOException {
        String pkg = args.get("package");
        if (pkg == null) {
            pkg = "/";
        }
        Scanner scanner = new Scanner(bundle, pkg);
        scanner.scan();
        final Set<Class<?>> classes = scanner.getClasses();
        return new Application() {
            @Override
            public Set<Class<?>> getClasses() {
                return classes;
            }
        };
    }

}
