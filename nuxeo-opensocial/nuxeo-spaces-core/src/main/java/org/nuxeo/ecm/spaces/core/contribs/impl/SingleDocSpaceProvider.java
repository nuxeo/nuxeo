package org.nuxeo.ecm.spaces.core.contribs.impl;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.spaces.api.AbstractSpaceProvider;
import org.nuxeo.ecm.spaces.api.Space;

public class SingleDocSpaceProvider extends AbstractSpaceProvider {

    @Override
    public void initialize(String... params) throws Exception {
        if(params.length == 0) {
            throw new Exception("Bad argument numbers for SingleDocSpaceProvider");
        }
    }


    public long size(CoreSession session) throws ClientException {
        // TODO Auto-generated method stub
        return 1;
    }

    public boolean isReadOnly() {
        return true;
    }


    public List<Space> getAll(CoreSession session) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }


    public Space getSpace(String spaceName, CoreSession session)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }


    public boolean isEmpty(CoreSession session) throws ClientException {
        // TODO Auto-generated method stub
        return false;
    }

}
