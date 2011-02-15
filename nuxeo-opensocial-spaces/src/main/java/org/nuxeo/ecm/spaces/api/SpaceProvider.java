package org.nuxeo.ecm.spaces.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;

public interface SpaceProvider {

    public void initialize(String name, Map<String, String> params)
            throws SpaceException;

    /**
     * Gets {@link org.nuxeo.ecm.spaces.api.SpaceProvider}'s name.
     *
     * @return provider name
     */
    String getName();

    Space getSpace(CoreSession session, DocumentModel contextDocument,
            String spaceName) throws SpaceException;

    List<Space> getAll(CoreSession session, DocumentModel contextDocument)
            throws SpaceException;

    long size(CoreSession session, DocumentModel contextDocument)
            throws SpaceException;

    boolean isEmpty(CoreSession session, DocumentModel contextDocument)
            throws SpaceException;

    boolean isReadOnly(CoreSession session);

    void add(Space o, CoreSession session, Map<String, String> params)
            throws SpaceException;

    void addAll(Collection<? extends Space> c, CoreSession session)
            throws SpaceException;

    boolean remove(Space space, CoreSession session) throws SpaceException;

    void clear(CoreSession session) throws SpaceException;

}
