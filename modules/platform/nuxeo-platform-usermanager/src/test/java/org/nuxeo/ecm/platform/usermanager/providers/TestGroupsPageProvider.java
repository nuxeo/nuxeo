/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */
package org.nuxeo.ecm.platform.usermanager.providers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, DirectoryFeature.class })
@Deploy("org.nuxeo.ecm.platform.usermanager")
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.platform.usermanager.tests:computedgroups-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.usermanager.tests:test-usermanagerimpl/directory-config.xml")
public class TestGroupsPageProvider {

    protected static final String PROVIDER_NAME = "groups_listing";

    @Inject
    protected PageProviderService ppService;

    @Inject
    protected UserManager userManager;

    @Before
    public void initGroups() {
        userManager.createGroup(createGroup("group1"));
        userManager.createGroup(createGroup("group2"));
    }

    @After
    public void cleanGroups() {
        userManager.deleteGroup("group1");
        userManager.deleteGroup("group2");
    }

    protected DocumentModel createGroup(String groupName) {
        DocumentModel newGroup = userManager.getBareGroupModel();
        newGroup.setProperty("group", "groupname", groupName);
        return newGroup;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGroupsPageProviderAllMode() {
        Map<String, Serializable> properties = new HashMap<>();
        properties.put(AbstractGroupsPageProvider.GROUPS_LISTING_MODE_PROPERTY, AbstractGroupsPageProvider.ALL_MODE);
        PageProvider<DocumentModel> groupsProvider = (PageProvider<DocumentModel>) ppService.getPageProvider(
                PROVIDER_NAME, null, null, null, properties, "");
        List<DocumentModel> groups = groupsProvider.getCurrentPage();
        assertNotNull(groups);
        assertEquals(5, groups.size());

        DocumentModel group = groups.get(0);
        assertEquals("administrators", group.getId());
        group = groups.get(1);
        assertEquals("group1", group.getId());
        group = groups.get(2);
        assertEquals("group2", group.getId());
        group = groups.get(3);
        assertEquals("members", group.getId());
        group = groups.get(4);
        assertEquals("powerusers", group.getId());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGroupsPageProviderSearchMode() {
        Map<String, Serializable> properties = new HashMap<>();
        properties.put(AbstractGroupsPageProvider.GROUPS_LISTING_MODE_PROPERTY,
                AbstractGroupsPageProvider.SEARCH_ONLY_MODE);
        PageProvider<DocumentModel> groupsProvider = (PageProvider<DocumentModel>) ppService.getPageProvider(
                PROVIDER_NAME, null, null, null, properties, "group");
        List<DocumentModel> groups = groupsProvider.getCurrentPage();
        assertNotNull(groups);
        assertEquals(2, groups.size());
        DocumentModel group = groups.get(0);
        assertEquals("group1", group.getId());
        group = groups.get(1);
        assertEquals("group2", group.getId());

        // check computed groups
        groupsProvider = (PageProvider<DocumentModel>) ppService.getPageProvider(PROVIDER_NAME, null, null, null,
                properties, "Grp");
        groups = groupsProvider.getCurrentPage();
        assertNotNull(groups);
        assertEquals(2, groups.size());
        group = groups.get(0);
        assertEquals("Grp1", group.getId());
        group = groups.get(1);
        assertEquals("Grp2", group.getId());

        // regular and computed groups together
        groupsProvider = (PageProvider<DocumentModel>) ppService.getPageProvider(PROVIDER_NAME, null, null, null,
                properties, "gr");
        groups = groupsProvider.getCurrentPage();
        assertEquals(4, groups.size());
        assertEquals("group1", groups.get(0).getId());
        assertEquals("group2", groups.get(1).getId());
        assertEquals("Grp1", groups.get(2).getId());
        assertEquals("Grp2", groups.get(3).getId());
    }

}
