package org.nuxeo.ecm.spaces.api;

import java.util.Collection;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;

public interface SpaceProvider {

    public void initialize(String... params) throws Exception;

    Space getSpace(String spaceName, CoreSession session) throws ClientException;

    List<Space> getAll(CoreSession session) throws ClientException;

    void add(Space o, CoreSession session) throws ClientException;

    void addAll(Collection<? extends Space> c, CoreSession session) throws ClientException;

    void clear( CoreSession session) throws ClientException;

    boolean isEmpty(CoreSession session) throws ClientException;

    boolean remove(Space space, CoreSession session) throws ClientException;

    long size(CoreSession session) throws ClientException;

    boolean isReadOnly();
}
