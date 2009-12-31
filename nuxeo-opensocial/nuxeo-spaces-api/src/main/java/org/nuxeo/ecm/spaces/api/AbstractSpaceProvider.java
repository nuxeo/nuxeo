package org.nuxeo.ecm.spaces.api;

import java.util.Collection;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceNotFoundException;

abstract public class AbstractSpaceProvider implements SpaceProvider {

    public void add(Space o, CoreSession session) throws SpaceException {
        if(isReadOnly()) throw new SpaceException("This SpaceProvider is read only");
    }

    public void addAll(Collection<? extends Space> c, CoreSession session)
            throws SpaceException {
        if(isReadOnly()) throw new SpaceException("This SpaceProvider is read only");

    }

    public void clear(CoreSession session) throws SpaceException {
        if(isReadOnly()) throw new SpaceException("This SpaceProvider is read only");

    }

    final public Space getSpace(String spaceName, CoreSession session) throws SpaceException{
        Space result = doGetSpace(spaceName, session);
        if(result == null) {
            throw new SpaceNotFoundException();
        } else {
            return result;
        }
    }


    abstract protected Space doGetSpace(String spaceName, CoreSession session) throws SpaceException;

    public void initialize(Map<String,String> params) throws Exception {

    }

    public boolean remove(Space space, CoreSession session)
            throws SpaceException {
        if(isReadOnly()) throw new SpaceException("This SpaceProvider is read only");
        return false;
    }



}
