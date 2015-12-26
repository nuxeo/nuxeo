/*
 * (C) Copyright 2006 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Bogdan Stefanescu
 */
package org.nuxeo.ecm.webengine.samples;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;

/**
 * UserBuddies object. You can see the @WebAdapter annotation that is defining a WebAdapter of type "UserBuddies" that
 * applies to any User WebObject. The name used to access this adapter is the adapter name prefixed with a '@'
 * character: {@code @buddies}
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@WebAdapter(name = "buddies", type = "UserBuddies", targetType = "User")
@Produces("text/html;charset=UTF-8")
public class UserBuddiesObject extends DefaultAdapter {

    /**
     * Get the index view. The view file name is computed as follows: index[-media_type_id].ftl First the
     * skin/views/UserBuddies is searched for that file then the current directory. (The type of a module is the same as
     * its name)
     */
    @GET
    public Object doGet() {
        return getView("index");
    }

}
