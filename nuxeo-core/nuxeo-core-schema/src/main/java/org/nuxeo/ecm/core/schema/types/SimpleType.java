/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.schema.types;

import org.nuxeo.ecm.core.schema.TypeRef;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface SimpleType extends Type {

    /**
     * Tests whether this type is a primitive type.
     *
     * @return true if this type is a primitive type, false otherwise
     */
    boolean isPrimitive();

    SimpleType getPrimitiveType();

    @Override
    TypeRef<? extends SimpleType> getRef();

}
