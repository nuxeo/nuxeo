/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.jbpm.core.pd;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.jbpm.core.helper.PublicationHelperImpl;

/**
 * @author arussel
 *
 */
public class PublicationPDTest extends AbstractProcessDefinitionTest {

    @Override
    public String getProcessDefinitionResource() {
        return "/process/publication.xml";
    }

    public void testPD() {
        List<DocumentModel> dms = getDocumentModelList();
        JbpmContext context = null;
        try {
            context = configuration.createJbpmContext();
            context.setActorId("bob");
            assertNotNull(context);
            context.deployProcessDefinition(pd);
            ProcessInstance pi = context.newProcessInstanceForUpdate("publication");
            pi.getContextInstance().setTransientVariable("sections", dms);
            pi.getContextInstance().setTransientVariable("session", getCoreSession());
            pi.getContextInstance().setTransientVariable("document", null);
            pi.getContextInstance().setVariable("publicationHelper",
                    new PublicationHelperImpl());
            TaskInstance ti = pi.getTaskMgmtInstance().createStartTaskInstance();
            ti.end();
            assertNotNull(pi);
        } finally {
            context.close();
        }

    }

    private CoreSession getCoreSession() {
        InvocationHandler handler = new InvocationHandler(){
            public Object invoke(Object proxy, Method method, Object[] args)
                    throws Throwable {
                if(method.getName().equals("toString")) {
                    return "mockSession";
                }
                return null;
            }
        };
        return (CoreSession) Proxy.newProxyInstance(CoreSession.class.getClassLoader(),
        new Class[] { CoreSession.class },  handler);
    }

    private List<DocumentModel> getDocumentModelList() {
        List<DocumentModel> dms = new ArrayList<DocumentModel>();
        for(int i = 0; i < 3; i++) {
            dms.add(getMockDocumentModel(i));
        }
        return dms;
    }

    private DocumentModel getMockDocumentModel(final int i) {
        InvocationHandler handler = new InvocationHandler(){
            public Object invoke(Object proxy, Method method, Object[] args)
                    throws Throwable {
                if(method.getName().equals("toString")) {
                    return "mockModel" + i;
                }
                return null;
            }
        };
        return (DocumentModel) Proxy.newProxyInstance(DocumentModel.class.getClassLoader(),
        new Class[] { DocumentModel.class },  handler);
    }
}
