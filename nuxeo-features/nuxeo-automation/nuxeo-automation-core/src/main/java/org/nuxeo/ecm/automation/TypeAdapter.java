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
 */
package org.nuxeo.ecm.automation;

/**
 * An object that can adapt a given object instance to an object of another
 * type A type adapter accepts only one type of objects and can produce only
 * one type of object
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface TypeAdapter {

    /**
     * Adapt the given object to an instance of the given target type. The
     * input object cannot be null. Throws an exception if the object cannot be
     * adapted.
     *
     * @param <T>
     * @param ctx
     * @param objectToAdapt
     * @throws TypeAdaptException when the object cannot be adapted
     */
    public Object getAdaptedValue(OperationContext ctx, Object objectToAdapt)
            throws TypeAdaptException;

}
