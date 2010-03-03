package org.nuxeo.ecm.spaces.core.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.spaces.api.Univers;
import org.nuxeo.ecm.spaces.api.exceptions.UniversNotFoundException;
import org.nuxeo.ecm.spaces.core.impl.docwrapper.VirtualUnivers;

public class VirtualUniversProvider implements UniversProvider {

    Map<String, Univers> universes = new HashMap<String, Univers>();

    public Univers getUnivers(String name, CoreSession session)
            throws ClientException {
        if (universes.containsKey(name)) {
            return universes.get(name);
        } else {
            throw new UniversNotFoundException();
        }
    }

    public void initialize(Map<String, String> params) throws Exception {
        if (!params.containsKey("names")) {
            throw new Exception("Unable to initialize virtual universe");
        }

        String names = params.get("names");
        if (names.indexOf("|") != -1) {
            for (String name : names.split("|")) {
                universes.put(name, new VirtualUnivers(name));
            }
        } else {
            universes.put(names, new VirtualUnivers(names));
        }
    }

    public List<Univers> getAll(CoreSession session) throws ClientException {
        List<Univers> result = new ArrayList<Univers>();
        result.addAll(universes.values());
        return result;
    }

}
