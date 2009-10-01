package org.nuxeo.opensocial.services.appdata;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.social.opensocial.spi.AppDataService;
import org.apache.shindig.social.opensocial.spi.DataCollection;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.SocialSpiException;
import org.apache.shindig.social.opensocial.spi.UserId;
import org.nuxeo.runtime.model.DefaultComponent;

public class AppDataServiceImpl extends DefaultComponent implements AppDataService  {

    private static final String NOT_IMPLEMENTED = "Not implemented";

    public Future<Void> deletePersonData(UserId userId, GroupId groupId,
            String appId, Set<String> fields, SecurityToken token)
             {
        throw new SocialSpiException(null, NOT_IMPLEMENTED);
    }

    public Future<DataCollection> getPersonData(Set<UserId> userIds,
            GroupId groupId, String appId, Set<String> fields,
            SecurityToken token)  {
        throw new SocialSpiException(null, NOT_IMPLEMENTED);
    }

    public Future<Void> updatePersonData(UserId userId, GroupId groupId,
            String appId, Set<String> fields, Map<String, String> values,
            SecurityToken token) {
        throw new SocialSpiException(null, NOT_IMPLEMENTED);
    }

}
