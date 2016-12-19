/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package info.simplecloud.scimproxy.compliance.test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;

import info.simplecloud.core.Group;
import info.simplecloud.core.Resource;
import info.simplecloud.core.User;
import info.simplecloud.scimproxy.compliance.CSP;
import info.simplecloud.scimproxy.compliance.enteties.TestResult;

/**
 * Overriding the default test just to solve resource resolution issue !
 *
 * @author tiry
 */
public class WorkingPostTest extends PostTest {

    public WorkingPostTest(CSP csp, ResourceCache<User> userCache, ResourceCache<Group> groupCache) {
        super(csp, userCache, groupCache);
    }

    protected User getUser() {
        try {
            InputStream in = this.getClass().getResourceAsStream("/user_full.json");
            String fullUser = IOUtils.toString(in, Charsets.UTF_8);
            // String fullUser = FileUtils.readFileToString(new File("src/main/resources/user_full.json"));
            return new User(fullUser, Resource.ENCODING_JSON);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<TestResult> run() {
        List<TestResult> results = new ArrayList<>();

        // simple user
        User scimUser = new User();
        Group scimGroup = new Group();

        // full user
        User scimUserFull = getUser();

        long nanoTime = System.nanoTime();
        // user
        scimUser.setUserName("J" + nanoTime);
        results.add(create("json", scimUser, false));
        scimUser.setUserName("Js" + nanoTime);
        results.add(create("json", scimUser, true));
        scimUserFull.setUserName("Jso" + nanoTime);
        results.add(create("json", scimUserFull, false));

        // group
        scimGroup.setDisplayName("ScimGroupJson");
        results.add(create("json", scimGroup, false));

        // run same tests but now with XML
        if (csp.getSpc().hasXmlDataFormat()) {
            // user
            scimUser.setUserName("X" + nanoTime);
            results.add(create("xml", scimUser, false));
            scimUser.setUserName("Xm" + nanoTime);
            results.add(create("xml", scimUser, true));
            scimUserFull.setUserName("Xml" + nanoTime);
            results.add(create("xml", scimUserFull, false));

            // group
            scimGroup.setDisplayName("ScimGroupXml");
            results.add(create("xml", scimGroup, false));
        }

        return results;
    }

}
