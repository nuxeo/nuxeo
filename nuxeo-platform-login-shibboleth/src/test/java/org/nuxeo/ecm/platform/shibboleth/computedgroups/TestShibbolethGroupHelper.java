/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Arnaud Kervern
 */

package org.nuxeo.ecm.platform.shibboleth.computedgroups;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.shibboleth.ShibbolethConstants;
import org.nuxeo.ecm.platform.shibboleth.ShibbolethGroupHelper;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

import static org.junit.Assert.assertSame;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(type = BackendType.H2, init = DefaultRepositoryInit.class, user = "Administrator", cleanup = Granularity.METHOD)
@Deploy( { "org.nuxeo.ecm.platform.content.template",
        "org.nuxeo.ecm.platform.dublincore", "org.nuxeo.ecm.directory.api",
        "org.nuxeo.ecm.directory.types.contrib", "org.nuxeo.ecm.directory",
        "org.nuxeo.ecm.directory.sql",
        "org.nuxeo.ecm.platform.login.shibboleth" })
@LocalDeploy("org.nuxeo.ecm.platform.login.shibboleth:OSGI-INF/test-sql-directory.xml")
public class TestShibbolethGroupHelper {

    @Inject
    protected CoreSession session;

    @Test
    public void testCreateGroup() throws Exception {
        assertSame(0, ShibbolethGroupHelper.getGroups().size());
        DocumentModel group = ShibbolethGroupHelper.getBareGroupModel(session);

        group.setPropertyValue("shibbGroup:groupName", "group1");
        group.setPropertyValue("shibbGroup:expressionLanguage",
                ShibbolethConstants.EL_CURRENT_USER_NAME);
        ShibbolethGroupHelper.createGroup(group);

        assertSame(1, ShibbolethGroupHelper.getGroups().size());
    }

    @Test
    public void testSearchGroup() throws Exception {
        getGroup("group2", true);
        getGroup("group3", true);
        getGroup("group4", true);
        getGroup("test", true);
        getGroup("group6", true);

        assertSame(1, ShibbolethGroupHelper.searchGroup("test").size());
        assertSame(6, ShibbolethGroupHelper.searchGroup("").size());
        assertSame(5, ShibbolethGroupHelper.searchGroup("group%").size());
        assertSame(5, ShibbolethGroupHelper.searchGroup("group").size());
    }

    protected DocumentModel getGroup(String name, boolean createIt)
            throws Exception {
        DocumentModel group = ShibbolethGroupHelper.getBareGroupModel(session);
        group.setPropertyValue("shibbGroup:groupName", name);
        group.setPropertyValue("shibbGroup:expressionLanguage", "currentUser");

        if (createIt) {
            group = ShibbolethGroupHelper.createGroup(group);
        }
        return group;
    }
}
