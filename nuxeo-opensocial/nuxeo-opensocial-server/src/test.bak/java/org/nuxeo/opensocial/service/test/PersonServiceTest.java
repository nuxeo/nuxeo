package org.nuxeo.opensocial.service.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.spi.PersonService;
import org.apache.shindig.social.opensocial.spi.UserId;
import org.apache.shindig.social.opensocial.spi.UserId.Type;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.opensocial.services.NuxeoServiceModule;
import org.nuxeo.runtime.api.Framework;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.NuxeoRunner;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.TestRuntimeHarness;

@RunWith(NuxeoRunner.class)
public class PersonServiceTest {

    private TestRuntimeHarness harness;
    private PersonService service;

    @Inject
    public PersonServiceTest(TestRuntimeHarness harness, UserManager um)
            throws Exception {
        this.harness = harness;
        assertNotNull(um);

        harness.deployBundle("org.nuxeo.opensocial.service");
        service = Framework.getService(PersonService.class);
        assertNotNull(service);

    }

    @Test
    public void iCanGetTheAdministratorPerson() throws Exception {

        Person person = service.getPerson(new UserId(Type.me, "Administrator"),
                null, null).get();

        assertNotNull(person);
        assertEquals("Administrator", person.getId());
    }

    @Test
    public void iCanCustomizeThePersonService() throws Exception {
        harness.deployContrib("org.nuxeo.opensocial.service.test",
                "OSGI-INF/person-service-contrib.xml");

        Person person = service.getPerson(new UserId(Type.me, "Administrator"),
                null, null).get();

        assertNotNull(person);
        assertEquals("Administrator", person.getId());

        assertEquals("Test Name", person.getName().getFamilyName());
        assertEquals("Test FirstName", person.getName().getGivenName());

    }

    @Test
    public void injectedServiceIsTheNuxeoComponent() throws Exception {
        Injector injector = Guice.createInjector(new NuxeoServiceModule());
        PersonService guicePs = injector.getInstance(PersonService.class);
        PersonService nuxeoPs = Framework.getService(PersonService.class);

        assertSame(nuxeoPs, guicePs);

    }
}
