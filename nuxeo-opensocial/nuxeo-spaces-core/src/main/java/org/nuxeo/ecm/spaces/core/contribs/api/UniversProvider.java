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
