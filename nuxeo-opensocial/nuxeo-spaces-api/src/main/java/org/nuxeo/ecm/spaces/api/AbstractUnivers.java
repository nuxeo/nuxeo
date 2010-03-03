package org.nuxeo.ecm.spaces.api;

import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.runtime.api.Framework;

public abstract class AbstractUnivers implements Univers {

    public List<Space> getSpaces(CoreSession session) throws SpaceException {
        SpaceManager sm;
        try {
            sm = Framework.getService(SpaceManager.class);
            return sm.getSpacesForUnivers(this, session);
        } catch (Exception e) {
            throw new SpaceException("Unable to get spaces", e);
        }

    }

    public List<SpaceProvider> getSpaceProviders(CoreSession session) throws SpaceException {
      SpaceManager sm;
      try {
          sm = Framework.getService(SpaceManager.class);
      return sm.getSpacesProvider(this);
      } catch (Exception e) {
          throw new SpaceException("Unable to get space providers",e);
      }
    }
}
