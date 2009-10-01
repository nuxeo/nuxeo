package org.nuxeo.opensocial.services.person;

import java.util.Set;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.spi.CollectionOptions;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.PersonService;
import org.apache.shindig.social.opensocial.spi.RestfulCollection;
import org.apache.shindig.social.opensocial.spi.SocialSpiException;
import org.apache.shindig.social.opensocial.spi.UserId;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;


public class PersonServiceImpl extends DefaultComponent implements
        PersonService {

    private PrincipalConverter converter = new DefaultPrincipalConverter();

    private static final Log LOG = LogFactory.getLog(PersonServiceImpl.class);

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if ("principalConverter".equals(extensionPoint)) {
            PrincipalConverterDescriptor converterDescriptor = (PrincipalConverterDescriptor) contribution;

                PrincipalConverter conv;
                try {
                    conv = converterDescriptor.getPrincipalConverterClass().newInstance();
                    converter = conv;
                } catch (InstantiationException e) {
                    LOG.error("Unable to get converter " + converterDescriptor.getPrincipalConverterClass().getCanonicalName());
                } catch (IllegalAccessException e) {
                    LOG.error("Unable to get converter " + converterDescriptor.getPrincipalConverterClass().getCanonicalName());
                }

        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        converter = new DefaultPrincipalConverter();

    }

    public Future<RestfulCollection<Person>> getPeople(Set<UserId> userIds,
            GroupId groupId, CollectionOptions collectionOptions,
            Set<String> fields, SecurityToken token)  {
        System.err.println("getPeople for " + userIds);
        return null;
    }

    private String realUid(UserId id, SecurityToken token) {
        return id.getUserId() == null ? id.getUserId(token) : id.getUserId();
    }

    public Future<Person> getPerson(UserId id, Set<String> fields,
            SecurityToken token)  {
        String userId = realUid(id, token);

        try {
            UserManager um = Framework.getService(UserManager.class);
            NuxeoPrincipal principal = um.getPrincipal(userId);
            return ImmediateFuture.newInstance(converter.convert(principal));
        } catch (Exception e) {
            throw new SocialSpiException(ResponseError.INTERNAL_ERROR,
                    "Unable to get user : " + e.getMessage(),e);
        }
    }
}
