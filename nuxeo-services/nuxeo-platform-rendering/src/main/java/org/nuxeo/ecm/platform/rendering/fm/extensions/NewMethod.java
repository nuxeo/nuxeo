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

package org.nuxeo.ecm.platform.rendering.fm.extensions;

import java.lang.reflect.Constructor;
import java.util.List;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class NewMethod implements TemplateMethodModelEx {

    public Object exec(List arguments) throws TemplateModelException {
        int size = arguments.size();
        if (size < 1) {
            throw new TemplateModelException(
                    "Invalid number of arguments for new(class, ...) method");
        }

        Class<?> klass;
        try {
            String className = (String)arguments.get(0);
            klass = Class.forName(className);
            if (size == 1) {
                return klass.newInstance();
            }
        } catch (Exception e) {
            throw new TemplateModelException("Failed to isntantiate the object", e);
        }
        arguments.remove(0);
        Object[] ar = arguments.toArray();
        size--;
        Constructor<?>[] ctors = klass.getConstructors();
        for (Constructor<?> ctor : ctors) {
            Class<?>[] params = ctor.getParameterTypes(); // this is cloning params
            if (params.length == size) { // try this one
                try {
                    return ctor.newInstance(ar);
                } catch (Throwable e) {
                    // continue
                }
            }
        }
        throw new TemplateModelException("No suitable constructor found");
    }

}
