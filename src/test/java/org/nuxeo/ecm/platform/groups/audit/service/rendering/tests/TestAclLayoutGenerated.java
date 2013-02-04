/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */

package org.nuxeo.ecm.platform.groups.audit.service.rendering.tests;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.groups.audit.service.rendering.AclExcelLayoutBuilder;
import org.nuxeo.ecm.platform.groups.audit.service.rendering.AclExcelLayoutBuilder2;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

/**
 * Test excel export of groups
 *
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.core", "org.nuxeo.ecm.core.api",
		"org.nuxeo.runtime.management", "org.nuxeo.ecm.directory.api",
		"org.nuxeo.ecm.directory", "org.nuxeo.ecm.directory.sql",
		"org.nuxeo.ecm.platform.usermanager",
		"org.nuxeo.ecm.platform.usermanager.api", "nuxeo-groups-rights-audit",
		"org.nuxeo.ecm.automation.core" })
@LocalDeploy({ "nuxeo-groups-rights-audit:OSGI-INF/directory-config.xml",
		"nuxeo-groups-rights-audit:OSGI-INF/schemas-config.xml",
		"nuxeo-groups-rights-audit:OSGI-INF/test-chain-export-operation.xml" })
public class TestAclLayoutGenerated extends AbstractAclLayoutTest {
	@Inject
	CoreSession session;
	@Inject
	UserManager userManager;
	@Inject
	AutomationService automationService;

	private final static Log log = LogFactory.getLog(TestAclLayoutGenerated.class);


	@Test
	public void testExcelExportReport() throws Exception {
		// doc tree
		int depth = 10;
		int width = 5;
		int groups = 25;

		System.out.println("Build  @ " + new Date());
		makeDocumentTree(session, depth, width, groups);

		// generate XLS report
		System.out.println("Start audit @ " + new Date());

		AclExcelLayoutBuilder v = new AclExcelLayoutBuilder2();
		v.renderAudit(session);

		System.out.println("End audit @ " + new Date());
		v.getExcel().save(
				TestAclLayoutGenerated.class.getSimpleName() + ".xls");
		System.out.println("End save @ " + new Date());
	}

	/* BUILD DOC TREE */

	protected DocumentModel makeDocumentTree(CoreSession session, int depth,
			int width, int nGroups) throws Exception {
		DocumentModel root = makeFolder(session, "/", "root");

		List<String> groups = new ArrayList<String>(nGroups);
		for (int i = 0; i < nGroups; i++) {
			String group = "group" + i;
			groups.add(group);
		}

		return makeDocumentTree(session, depth, width, nGroups, 1, root, groups);
	}

	protected DocumentModel makeDocumentTree(CoreSession session, int maxDepth,
			int width, int nGroups, int currentDepth, DocumentModel folder,
			List<String> groups) throws PropertyException, ClientException {

		// populate current folder with random ACL
		for (String group : groups) {
			addAcl(session, folder, group, SecurityConstants.READ_WRITE, true);
			addAcl(session, folder, group, SecurityConstants.ADD_CHILDREN, true);
			addAcl(session, folder, group, SecurityConstants.REMOVE_CHILDREN, true);
			addAcl(session, folder, group, SecurityConstants.READ_LIFE_CYCLE, true);
			addAcl(session, folder, group, SecurityConstants.WRITE_SECURITY, true);

			// final rule with lock inherit
			if(currentDepth!=0 && currentDepth%4==0)
				addAclLockInheritance(session, folder, group);
		}

		// generate children folders
		if (currentDepth < maxDepth) {
			// dispatch all groups into each tree branch
			List<List<String>> subgroups;

			int subgroupSize = groups.size() / width;
			if (subgroupSize > 0) { // general case
				//System.out.println(subgroupSize + "|" + groups);
				subgroups = Lists.partition(groups, subgroupSize);
			} else {
				if (groups.size() >= 2) {
					subgroups = Lists.partition(groups, 1);
				} else {
					subgroups = new ArrayList<List<String>>();
					subgroups.add(groups);
				}
			}

			for (int i = 0; i < width; i++) {
				// create a folder
				String name = "[" + currentDepth + "]folder-" + i;
				DocumentModel f = makeFolder(session, folder.getPathAsString(),
						name);

				//System.out.println("created " + folder.getPathAsString() + "/"
				//		+ name);


				// generate children folders
				if (i < subgroups.size()) {
					List<String> subgroup = subgroups.get(i);
					makeDocumentTree(session, maxDepth, width, nGroups,
							currentDepth + 1, f, subgroup);
				}
				else{
					makeDocumentTree(session, maxDepth, width, nGroups,
							currentDepth + 1, f, new ArrayList<String>());

				}
			}
			//System.out.println("done " + folder.getPathAsString() + "/");

			return folder;
		}
		// or end recursion
		else
			return folder;
	}
}