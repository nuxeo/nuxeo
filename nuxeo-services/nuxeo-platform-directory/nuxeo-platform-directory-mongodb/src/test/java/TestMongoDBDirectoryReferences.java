/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.mongodb.MongoDBDirectory;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.directory.Reference;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @since 9.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@LocalDeploy("org.nuxeo.directory.mongodb.tests:test-mongodb-directory-with-references.xml")
public class TestMongoDBDirectoryReferences extends MongoDBDirectoryTestCase {

    @Test
    public void testReferences() {

        MongoDBDirectory dir = getDirectory(GROUP_DIR);
        Reference membersRef = dir.getReferences("members").get(0);

        // test initial configuration
        List<String> administrators = membersRef.getTargetIdsForSource("administrators");
        assertEquals(1, administrators.size());
        assertTrue(administrators.contains("Administrator"));

        // add user_1 to the administrators group
        membersRef.addLinks("administrators", Arrays.asList("user_1"));

        administrators = membersRef.getTargetIdsForSource("administrators");
        assertEquals(2, administrators.size());
        assertTrue(administrators.contains("Administrator"));
        assertTrue(administrators.contains("user_1"));

        // reading the same link should not duplicate it
        membersRef.addLinks("administrators", Arrays.asList("user_1", "user_2"));

        administrators = membersRef.getTargetIdsForSource("administrators");
        assertEquals(3, administrators.size());
        assertTrue(administrators.contains("Administrator"));
        assertTrue(administrators.contains("user_1"));
        assertTrue(administrators.contains("user_2"));

        // remove the reference to Administrator
        membersRef.removeLinksForTarget("Administrator");
        administrators = membersRef.getTargetIdsForSource("administrators");
        assertEquals(2, administrators.size());
        assertTrue(administrators.contains("user_1"));
        assertTrue(administrators.contains("user_2"));

        // remove the references from administrators
        membersRef.removeLinksForSource("administrators");
        administrators = membersRef.getTargetIdsForSource("administrators");
        assertEquals(0, administrators.size());

        membersRef.setTargetIdsForSource("administrators", Arrays.asList("user_1", "user_2"));
        administrators = membersRef.getTargetIdsForSource("administrators");
        assertEquals(2, administrators.size());
        assertTrue(administrators.contains("user_1"));
        assertTrue(administrators.contains("user_2"));

        membersRef.setTargetIdsForSource("administrators", Arrays.asList("user_1", "Administrator"));
        administrators = membersRef.getTargetIdsForSource("administrators");
        assertEquals(2, administrators.size());
        assertTrue(administrators.contains("user_1"));
        assertTrue(administrators.contains("Administrator"));

        membersRef.setSourceIdsForTarget("Administrator", Arrays.asList("members"));
        administrators = membersRef.getTargetIdsForSource("administrators");
        assertEquals(1, administrators.size());
        assertTrue(administrators.contains("user_1"));

        administrators = membersRef.getSourceIdsForTarget("Administrator");
        assertEquals(1, administrators.size());
        assertTrue(administrators.contains("members"));
    }

}
