package org.nuxeo.ecm.spaces.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;

public interface SpaceProvider {

    public void initialize(Map<String, String> params) throws Exception;

    Space getSpace(String spaceName, CoreSession session) throws SpaceException;

    List<Space> getAll(CoreSession session) throws SpaceException;

    void add(Space o, CoreSession session) throws SpaceException;

    void addAll(Collection<? extends Space> c, CoreSession session)
            throws SpaceException;

    void clear(CoreSession session) throws SpaceException;

    boolean isEmpty(CoreSession session) throws SpaceException;

    boolean remove(Space space, CoreSession session) throws SpaceException;

    long size(CoreSession session) throws SpaceException;

    boolean isReadOnly(CoreSession session);
    
    /**
     * Gets {@link SpaceProvider}'s name.
     * 
     * @return provider name
     * @throws SpaceException if space manager is not reachable
     */
    String getName() throws SpaceException;

    /**
     * Gets all {@link Space}s supplied by {@link SpaceProvider}. This method does not throw any exception,
     * it can be useful in some cases.
     * @param session core session
     * @return list of {@link Space} OR an empty list if no spaces are supplied by space provider (never returns null)
     */
    List<Space> getAllSpaces(CoreSession session);
}
