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
package org.nuxeo.ecm.webengine.jaxrs.scan;

import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.nuxeo.ecm.webengine.jaxrs.ApplicationFactory;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DynamicApplicationFactory implements ApplicationFactory {

    @Override
    public Application getApplication(Bundle bundle, Map<String, String> args)
    throws Exception {
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
