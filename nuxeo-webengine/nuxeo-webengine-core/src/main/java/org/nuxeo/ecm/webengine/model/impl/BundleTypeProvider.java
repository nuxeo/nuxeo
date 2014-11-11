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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.model.impl;

import org.nuxeo.ecm.webengine.loader.ClassProxy;
import org.nuxeo.ecm.webengine.loader.StaticClassProxy;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.runtime.annotations.loader.AnnotationLoader;
import org.nuxeo.runtime.annotations.loader.BundleAnnotationsLoader;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class BundleTypeProvider extends TypeConfigurationProvider implements AnnotationLoader {


    public BundleTypeProvider() {
        BundleAnnotationsLoader.getInstance().addLoader(WebObject.class.getName(), this);
        BundleAnnotationsLoader.getInstance().addLoader(WebAdapter.class.getName(), this);
    }

    // support for loading annotations from descriptor files
    public void loadAnnotation(Bundle bundle, String annotationType, String className, String[] args) throws Exception {
        // args are ignored for now
        ClassProxy clazz = new StaticClassProxy(bundle.loadClass(className));
        if (annotationType.equals(WebObject.class.getName())) {
            WebObject type = clazz.get().getAnnotation(WebObject.class);
            registerType(TypeDescriptor.fromAnnotation(clazz, type));
        } else if (annotationType.equals(WebAdapter.class.getName())) {
            //TODO: avoid loading clazz here - use the data from annotation ?
            WebAdapter service = clazz.get().getAnnotation(WebAdapter.class);
            registerType(AdapterDescriptor.fromAnnotation(clazz, service));
        } else {
            throw new IllegalArgumentException(annotationType);
        }
    }

}
