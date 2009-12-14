package org.nuxeo.ecm.spaces.api;

import java.util.List;

public interface SpaceProvider extends List<Space> {

    void getSpace(String spaceName);

}
