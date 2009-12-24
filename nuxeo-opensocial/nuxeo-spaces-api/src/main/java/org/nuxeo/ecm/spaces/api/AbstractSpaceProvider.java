package org.nuxeo.ecm.spaces.api;

import java.util.Collection;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;

abstract public class AbstractSpaceProvider implements SpaceProvider {

    public void add(Space o, CoreSession session) throws ClientException {
        if(isReadOnly()) throw new ClientException("This SpaceProvider is read only");
    }

    public void addAll(Collection<? extends Space> c, CoreSession session)
            throws ClientException {
        if(isReadOnly()) throw new ClientException("This SpaceProvider is read only");

    }

    public void clear(CoreSession session) throws ClientException {
        if(isReadOnly()) throw new ClientException("This SpaceProvider is read only");

    }


    public void initialize(Map<String,String> params) throws Exception {

    }

    public boolean remove(Space space, CoreSession session)
            throws ClientException {
        if(isReadOnly()) throw new ClientException("This SpaceProvider is read only");
        return false;
    }



}
