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

import java.util.Set;

import javax.ws.rs.core.Application;

import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class DynamicApplication extends Application {

    protected abstract Bundle getBundle();

    protected String getPackageBase() {
        String packageBase = getClass().getName();
        int i = packageBase.lastIndexOf('.');
        if (i > -1) {
            packageBase = packageBase.substring(0, i);
        }
        return packageBase.replace('.', '/');
    }

    @Override
    public Set<Class<?>> getClasses() {
        try {
            Scanner scanner = new Scanner(getBundle(), getPackageBase());
            scanner.scan();
            return scanner.getClasses();
        } catch (Exception e) {
            throw new RuntimeException("Failed to scan classes", e);
        }
    }

}
