package org.nuxeo.ecm.spaces.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceNotFoundException;

abstract public class AbstractSpaceProvider implements SpaceProvider {

    protected String name;

    @Override
    public void initialize(String name, Map<String, String> params)
            throws SpaceException {
        this.name = name;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.nuxeo.ecm.spaces.api.SpaceProvider#getName()
     */
    final public String getName() {
        return this.name;
    }

    @Override
    final public Space getSpace(CoreSession session,
            DocumentModel contextDocument, String spaceName, Map<String, String> parameters)
            throws SpaceException {
        if (parameters == null) {
            parameters = new HashMap<String, String>();
        }
        Space result = doGetSpace(session, contextDocument, spaceName, parameters);
        if (result == null) {
            throw new SpaceNotFoundException();
        } else {
            return result;
        }
    }

    @Override
    final public Space getSpace(CoreSession session,
            DocumentModel contextDocument, String spaceName)
            throws SpaceException {
        return getSpace(session, contextDocument, spaceName, new HashMap<String, String>());
    }

    abstract protected Space doGetSpace(CoreSession session,
            DocumentModel contextDocument, String spaceName, Map<String, String> parameters)
            throws SpaceException;

    public List<Space> getAll(CoreSession session, DocumentModel contextDocument)
            throws SpaceException {
        List<Space> result = new ArrayList<Space>();
        result.add(getSpace(session, contextDocument, null));
        return result;
    }

    @Override
    public boolean isEmpty(CoreSession session, DocumentModel contextDocument)
            throws SpaceException {
        return getAll(session, contextDocument).isEmpty();
    }

    @Override
    public long size(CoreSession session, DocumentModel contextDocument)
            throws SpaceException {
        return getAll(session, contextDocument).size();
    }

    public void add(Space o, CoreSession session, Map<String, String> params)
            throws SpaceException {
        if (isReadOnly(session))
            throw new SpaceException("This SpaceProvider is read only");
    }

    public void addAll(Collection<? extends Space> c, CoreSession session)
            throws SpaceException {
        if (isReadOnly(session))
            throw new SpaceException("This SpaceProvider is read only");
    }

    public boolean remove(Space space, CoreSession session)
            throws SpaceException {
        if (isReadOnly(session))
            throw new SpaceException("This SpaceProvider is read only");
        return false;
    }

    public void clear(CoreSession session) throws SpaceException {
        if (isReadOnly(session))
            throw new SpaceException("This SpaceProvider is read only");

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.nuxeo.ecm.spaces.api.SpaceProvider#getSpaces(org.nuxeo.ecm.spaces
     * .api.Space, org.nuxeo.ecm.core.api.CoreSession)
     */
    public List<Space> getSpaces(Space space, CoreSession session) {
        // TODO Auto-generated method stub
        // if (space == null) {
        // return getAllSpaces(session);
        // }
        return new ArrayList<Space>();
    }

}
