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

package org.nuxeo.ecm.spaces.core.impl;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.ecm.spaces.core.impl.exceptions.NoElementFoundException;
import org.nuxeo.ecm.spaces.core.impl.exceptions.OperationNotSupportedException;
/**
 * Generic provider of elements E for type parent P
 * @author 10044893
 *
 * @param <E> element type
 * @param <P> parent type
 */
public interface GenericProvider<E,P> {

  /**
   * List elements for a parent in a core session
   * @param parent
   * @param session
   * @return
   * @throws ClientException
   */
  List<? extends E> getElementsForParent(P parent, CoreSession session) throws OperationNotSupportedException,SpaceException;

  /**
   * Retrieve a element from its name
   * @param name name of the element to be retrieved
   * @param parent parent element
   * @param session current core session
   * @return
   * @throws ClientException
   */
  E getElement(String name, P parent, CoreSession session) throws NoElementFoundException,OperationNotSupportedException,SpaceException;

  /**
   * Removal of an element
   * @param element
   * @param session
   * @return
   * @throws ClientException
   */
  void delete(E element, CoreSession session) throws OperationNotSupportedException,SpaceException;

  /**
   * Update datas of an element
   * @param newDatas
   * @param session
   * @return
   * @throws ClientException
   */
  E update(E newDatas, CoreSession session)throws OperationNotSupportedException,SpaceException;

  /**
   * Creation of an element
   * @param data
   * @param session
   * @return
   * @throws ClientException
   */
  E create(E data,P parent,CoreSession session)throws OperationNotSupportedException,SpaceException;

  /**
   * Category of the provider , will gives the category of the elements managed by this provider
   * @return
   */
  String getCategory();





}
