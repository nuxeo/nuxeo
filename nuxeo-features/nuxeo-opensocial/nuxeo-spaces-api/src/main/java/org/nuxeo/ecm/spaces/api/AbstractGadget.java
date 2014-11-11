package org.nuxeo.ecm.spaces.api;

import org.nuxeo.ecm.core.api.ClientException;

public abstract class AbstractGadget implements Gadget {
    public String getPref(String prefKey) throws ClientException {
        return getPreferences().get(prefKey);
    }
}
