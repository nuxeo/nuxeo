package org.nuxeo.opensocial.services.person;

import org.apache.shindig.social.opensocial.model.Person;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

public interface PrincipalConverter {

    /**
     * Converts a Nuxeo Principal to an OpenSocial Person
     * @param principal
     * @return
     */
    Person convert(NuxeoPrincipal principal);
}
