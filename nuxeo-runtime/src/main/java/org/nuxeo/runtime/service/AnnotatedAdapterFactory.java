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

package org.nuxeo.runtime.service;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class AnnotatedAdapterFactory<O> implements AdapterFactory<O> {

    @Override
    @SuppressWarnings("unchecked")
    public Class<O> getAdaptableType() {
        Adapter anno = getClass().getAnnotation(Adapter.class);
        if (anno == null) {
            throw new IllegalStateException(
                    "Invalid AnnotatedAdapterFactory class: "+getClass().getName()+". Must be annotated with Adapter annotation");
        }
        return (Class<O>)anno.type();
    }

    @Override
    public Class<?>[] getAdapterTypes() {
        Adapter anno = getClass().getAnnotation(Adapter.class);
        if (anno == null) {
            throw new IllegalStateException(
                    "Invalid AnnotatedAdapterFactory class. Must be annotated with Adapter annotation");
        }
        return anno.interfaces();
    }

}
