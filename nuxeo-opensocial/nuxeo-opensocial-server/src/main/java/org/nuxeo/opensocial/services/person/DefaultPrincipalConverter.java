/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.services.person;

import java.util.Collections;

import org.apache.shindig.social.core.model.NameImpl;
import org.apache.shindig.social.core.model.OrganizationImpl;
import org.apache.shindig.social.core.model.PersonImpl;
import org.apache.shindig.social.opensocial.model.Organization;
import org.apache.shindig.social.opensocial.model.Person;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

public class DefaultPrincipalConverter implements PrincipalConverter {

    public Person convert(NuxeoPrincipal principal) {

        PersonImpl person = new PersonImpl();

        person.setId(principal.getName());

        NameImpl name = new NameImpl();
        name.setFamilyName(principal.getLastName());
        name.setGivenName(principal.getFirstName());
        person.setName(name);

        String company = principal.getCompany();
        if (company != null) {
            Organization organization = new OrganizationImpl();
            organization.setName(company);
            person.setOrganizations(Collections.singletonList(organization));
        }

        return person;
    }

}
