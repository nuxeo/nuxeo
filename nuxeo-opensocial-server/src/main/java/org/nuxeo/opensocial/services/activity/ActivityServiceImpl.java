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

package org.nuxeo.opensocial.services.activity;

import java.util.Set;
import java.util.concurrent.Future;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.spi.ActivityService;
import org.apache.shindig.social.opensocial.spi.CollectionOptions;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.UserId;
import org.nuxeo.runtime.model.DefaultComponent;

public class ActivityServiceImpl extends DefaultComponent implements
        ActivityService {

    private static final String NOT_IMPLEMENTED = "Not implemented";

    public Future<Void> createActivity(UserId arg0, GroupId arg1, String arg2,
            Set<String> arg3, Activity arg4, SecurityToken arg5)
            throws ProtocolException {
        throw new ProtocolException(500, NOT_IMPLEMENTED);
    }

    public Future<Void> deleteActivities(UserId arg0, GroupId arg1,
            String arg2, Set<String> arg3, SecurityToken arg4)
            throws ProtocolException {
        throw new ProtocolException(500, NOT_IMPLEMENTED);
    }

    public Future<org.apache.shindig.protocol.RestfulCollection<Activity>> getActivities(
            Set<UserId> arg0, GroupId arg1, String arg2, Set<String> arg3,
            CollectionOptions arg4, SecurityToken arg5)
            throws ProtocolException {
        throw new ProtocolException(500, NOT_IMPLEMENTED);
    }

    public Future<org.apache.shindig.protocol.RestfulCollection<Activity>> getActivities(
            UserId arg0, GroupId arg1, String arg2, Set<String> arg3,
            CollectionOptions arg4, Set<String> arg5, SecurityToken arg6)
            throws ProtocolException {
        throw new ProtocolException(500, NOT_IMPLEMENTED);
    }

    public Future<Activity> getActivity(UserId arg0, GroupId arg1, String arg2,
            Set<String> arg3, String arg4, SecurityToken arg5)
            throws ProtocolException {
        throw new ProtocolException(500, NOT_IMPLEMENTED);
    }

}
