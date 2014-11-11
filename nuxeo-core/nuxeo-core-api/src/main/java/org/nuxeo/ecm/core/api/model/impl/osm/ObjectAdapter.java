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

package org.nuxeo.ecm.core.api.model.impl.osm;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface ObjectAdapter extends Serializable {

    Object create(Map<String, Object> value);

    Map<String, Object> getMap(Object object) throws PropertyException;

    void setMap(Object object, Map<String, Object> value) throws PropertyException;

    Object getValue(Object object, String name) throws PropertyException;

    void setValue(Object object, String name, Object value) throws PropertyException;

    ObjectAdapter getAdapter(String name) throws PropertyNotFoundException;

    Serializable getDefaultValue();

}
