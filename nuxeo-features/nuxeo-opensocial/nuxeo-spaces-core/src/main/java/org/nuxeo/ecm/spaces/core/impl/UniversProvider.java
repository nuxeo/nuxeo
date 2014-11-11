package org.nuxeo.ecm.spaces.core.impl;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.spaces.api.Univers;

public interface UniversProvider {

    void initialize(Map<String, String> params) throws Exception;

    Univers getUnivers(String name, CoreSession session) throws ClientException;

    List<Univers> getAll(CoreSession session) throws ClientException;

}
