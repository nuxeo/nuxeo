/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.ecm.spaces.core.contribs.api;

import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.spaces.api.Univers;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.ecm.spaces.core.impl.exceptions.NoElementFoundException;
import org.nuxeo.ecm.spaces.core.impl.exceptions.OperationNotSupportedException;


public interface UniversProvider  {

  Univers create(Univers data,CoreSession session) throws OperationNotSupportedException,SpaceException;

  Univers update(Univers univers,CoreSession session)throws OperationNotSupportedException,SpaceException;

  List<Univers> getAllElements(CoreSession session)throws OperationNotSupportedException,SpaceException;

  Univers getElementByName(String name,CoreSession session)throws OperationNotSupportedException,NoElementFoundException, SpaceException;

  void delete(Univers univers,CoreSession session)throws OperationNotSupportedException,SpaceException;

}
