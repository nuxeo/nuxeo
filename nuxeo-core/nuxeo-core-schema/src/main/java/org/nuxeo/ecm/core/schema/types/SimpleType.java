/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.schema.types;

/**
 * Simple Type.
 * <p>
 * May be primitive or not (in which case it has additional constraints over a
 * primitive type).
 */
public interface SimpleType extends Type {

    /**
     * Tests whether this type is a primitive type.
     *
     * @return true if this type is a primitive type, false otherwise
     */
    boolean isPrimitive();

    SimpleType getPrimitiveType();

}
