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
package org.nuxeo.ecm.automation;

/**
 * An object that can adapt a given object instance to an object of another
 * type A type adapter accepts only one type of objects and can produce only
 * one type of object.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface TypeAdapter {

    /**
     * Adapt the given object to an instance of the given target type. The
     * input object cannot be null. Throws an exception if the object cannot be
     * adapted.
     *
     * @param ctx
     * @param objectToAdapt
     * @throws TypeAdaptException when the object cannot be adapted
     */
    Object getAdaptedValue(OperationContext ctx, Object objectToAdapt)
            throws TypeAdaptException;

}
