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

package org.nuxeo.ecm.core.api.model.impl.osm.util;

import java.io.Serializable;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface MemberAccessor extends Serializable {

    boolean isReadOnly();

    void set(Object instance, Object value) throws AccessException;

    Object get(Object instance) throws AccessException;

    Class<?> getType();

    Class<?> getDeclaringClass();

}
