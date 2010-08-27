/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.spaces.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceNotFoundException;
import org.nuxeo.runtime.api.Framework;

abstract public class AbstractSpaceProvider implements SpaceProvider {

    private static final String COLUMN_ID_PREFIX = "column-";

    public void add(Space o, CoreSession session) throws SpaceException {
        if (isReadOnly(session))
            throw new SpaceException("This SpaceProvider is read only");
    }

    public void addAll(Collection<? extends Space> c, CoreSession session) throws SpaceException {
        if (isReadOnly(session))
            throw new SpaceException("This SpaceProvider is read only");

    }

    public void clear(CoreSession session) throws SpaceException {
        if (isReadOnly(session))
            throw new SpaceException("This SpaceProvider is read only");

    }

    final public Space getSpace(String spaceName, CoreSession session)
            throws SpaceException {
        Space result = doGetSpace(spaceName, session);
        if (result == null) {
            throw new SpaceNotFoundException();
        } else {
            return result;
        }
    }

    public boolean isEmpty(CoreSession session) throws SpaceException {
        return getAll(session).size() == 0;
    }

    public long size(CoreSession session) throws SpaceException {
        // TODO Auto-generated method stub
        return getAll(session).size();
    }

    abstract protected Space doGetSpace(String spaceName, CoreSession session)
            throws SpaceException;

    public void initialize(Map<String, String> params) throws Exception {

    }

    public boolean remove(Space space, CoreSession session) throws SpaceException {
        if(isReadOnly(session)) throw new SpaceException("This SpaceProvider is read only");
        return false;
    }

    final public String getName() throws SpaceException {
        SpaceManager sm;
        try {
            sm = Framework.getService(SpaceManager.class);
        } catch (Exception e) {
            throw new SpaceException("Unable to get Space Manager", e);
        }
        return sm.getProviderName(this);
    }

    final public List<Space> getAllSpaces(CoreSession session) {
        try {
            List<Space> list = getAll(session);
            if (list == null) {
                return new ArrayList<Space>();
            } else {
                return list;
            }
        } catch (Exception e) {
            return new ArrayList<Space>();
        }
    }

    public static String getColumnId(int n) {
        return COLUMN_ID_PREFIX + (n + 1);
    }

}
