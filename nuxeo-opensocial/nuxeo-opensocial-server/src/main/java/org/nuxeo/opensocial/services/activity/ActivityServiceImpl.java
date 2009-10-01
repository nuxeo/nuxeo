package org.nuxeo.opensocial.services.activity;

import java.util.Set;
import java.util.concurrent.Future;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.spi.ActivityService;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.RestfulCollection;
import org.apache.shindig.social.opensocial.spi.SocialSpiException;
import org.apache.shindig.social.opensocial.spi.UserId;
import org.nuxeo.runtime.model.DefaultComponent;

public class ActivityServiceImpl extends DefaultComponent implements
        ActivityService {

    private static final String NOT_IMPLEMENTED = "Not implemented";

    public Future<Void> createActivity(UserId userId, GroupId groupId,
            String appId, Set<String> fields, Activity activity,
            SecurityToken token) {
        throw new SocialSpiException(null, NOT_IMPLEMENTED);
    }

    public Future<Void> deleteActivities(UserId userId, GroupId groupId,
            String appId, Set<String> activityIds, SecurityToken token) {
        throw new SocialSpiException(null, NOT_IMPLEMENTED);
    }

    public Future<RestfulCollection<Activity>> getActivities(
            Set<UserId> userIds, GroupId groupId, String appId,
            Set<String> fields, SecurityToken token) {
        throw new SocialSpiException(null, NOT_IMPLEMENTED);
    }

    public Future<RestfulCollection<Activity>> getActivities(UserId userId,
            GroupId groupId, String appId, Set<String> fields,
            Set<String> activityIds, SecurityToken token) {
        throw new SocialSpiException(null, NOT_IMPLEMENTED);
    }

    public Future<Activity> getActivity(UserId userId, GroupId groupId,
            String appId, Set<String> fields, String activityId,
            SecurityToken token) {
        throw new SocialSpiException(null, NOT_IMPLEMENTED);
    }

}
