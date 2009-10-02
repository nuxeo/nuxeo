package org.nuxeo.opensocial.service.test;

import org.apache.shindig.social.core.model.NameImpl;
import org.apache.shindig.social.core.model.PersonImpl;
import org.apache.shindig.social.opensocial.model.Person;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.opensocial.services.person.PrincipalConverter;

import edu.emory.mathcs.backport.java.util.Collections;

public class TestPersonConverter implements PrincipalConverter {
    @SuppressWarnings("unchecked")
    public Person convert(NuxeoPrincipal principal) {

        PersonImpl person = new PersonImpl();

        person.setId(principal.getName());

        NameImpl name = new NameImpl();
        name.setFamilyName("Test Name");
        name.setGivenName("Test FirstName");
        person.setName(name);

        String company = principal.getCompany();
        if (company != null) {
            person.setOrganizations(Collections.singletonList(company));
        }

        return person;
    }

}
