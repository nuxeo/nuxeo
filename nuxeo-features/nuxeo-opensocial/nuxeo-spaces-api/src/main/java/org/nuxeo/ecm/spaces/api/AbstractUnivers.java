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

import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.runtime.api.Framework;

public abstract class AbstractUnivers implements Univers {

    public List<Space> getSpaces(CoreSession session) throws SpaceException {
        SpaceManager sm;
        try {
            sm = Framework.getService(SpaceManager.class);
            return sm.getSpacesForUnivers(this, session);
        } catch (Exception e) {
            throw new SpaceException("Unable to get spaces", e);
        }

    }

    public List<SpaceProvider> getSpaceProviders(CoreSession session) throws SpaceException {
      SpaceManager sm;
      try {
          sm = Framework.getService(SpaceManager.class);
      return sm.getSpacesProvider(this);
      } catch (Exception e) {
          throw new SpaceException("Unable to get space providers",e);
      }
    }
}
