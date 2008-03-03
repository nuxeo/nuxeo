/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: AbstractWorkflowDocumentRightsPolicyServiceTestCase.java 19119 2007-05-22 11:39:21Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.document;

import org.nuxeo.ecm.platform.workflow.document.service.WorkflowDocumentSecurityPolicyService;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public abstract class AbstractWorkflowDocumentRightsPolicyServiceTestCase
        extends NXRuntimeTestCase {

    protected WorkflowDocumentSecurityPolicyService service;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deploy("WorkflowDocumentSecurityPolicyService.xml");
        deploy("WorkflowDocumentSecurityPolicyServiceTestExtensions.xml");
        service = NXWorkflowDocument
                .getWorkflowDocumentRightsPolicyService();

    }

    public void testService() {
        assertNotNull(service);
    }

    public void testPolicyRegistration() {
        assertNull(service.getWorkflowDocumentSecurityPolicyByName("doesnotexist"));
        assertNotNull(service.getWorkflowDocumentSecurityPolicyByName("foo"));
        assertNotNull(service.getWorkflowDocumentSecurityPolicyByName("bar"));
    }

    public void testPolicyWorkflowMappings() {
        assertNull(service.getWorkflowDocumentSecurityPolicyFor("doesnotexist"));
        assertEquals("bar", service.getWorkflowDocumentSecurityPolicyFor("C").getName());
        assertEquals("foo", service.getWorkflowDocumentSecurityPolicyFor("A").getName());
        assertEquals("foo", service.getWorkflowDocumentSecurityPolicyFor("B").getName());
    }

}
