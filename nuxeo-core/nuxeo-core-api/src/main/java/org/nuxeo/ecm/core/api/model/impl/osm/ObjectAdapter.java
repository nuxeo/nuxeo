/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
