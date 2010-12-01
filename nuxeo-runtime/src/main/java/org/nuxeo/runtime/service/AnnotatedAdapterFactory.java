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
