/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.apidoc.seam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.jboss.seam.Component;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.contexts.ServletLifecycle;
import org.jboss.seam.web.ServletContexts;
import org.nuxeo.apidoc.api.SeamComponentInfo;
import org.nuxeo.apidoc.introspection.SeamComponentInfoImpl;

public class SeamRuntimeIntrospector {

    protected static List<String> listAllComponentsNames() {
        List<String> names = new ArrayList<>();
        if (Contexts.isApplicationContextActive()) {
            for (String name : Contexts.getApplicationContext().getNames()) {
                if (name.endsWith(".component")) {
                    names.add(name.replace(".component", ""));
                }
            }
        }
        return names;
    }

    // called by reflection from org.nuxeo.apidoc.introspection.RuntimeSnapshot#initSeamComponents
    public static List<SeamComponentInfo> listNuxeoComponents(HttpServletRequest request) {

        ServletLifecycle.beginRequest(request);
        ServletContexts.instance().setRequest(request);
        // ConversationPropagation.instance().setConversationId( conversationId
        // );
        // Manager.instance().restoreConversation();
        // ServletLifecycle.resumeConversation(request);

        try {
            return listNuxeoComponents();
        } finally {
            ServletLifecycle.endRequest(request);
        }

    }

    protected static List<SeamComponentInfo> components = null;

    protected static synchronized List<SeamComponentInfo> listNuxeoComponents() {
        if (components == null) {
            components = new ArrayList<>();
            for (String cName : listAllComponentsNames()) {
                SeamComponentInfoImpl desc = new SeamComponentInfoImpl();
                Component comp = Component.forName(cName);
                String className = comp.getBeanClass().getName();
                // if (className.startsWith("org.nuxeo")) {
                if (!className.startsWith("org.jboss")) {
                    desc.setName(cName);
                    desc.setScope(comp.getScope().toString());
                    desc.setClassName(className);

                    @SuppressWarnings("rawtypes")
                    Set<Class> ifaces = comp.getBusinessInterfaces();
                    if (ifaces != null && ifaces.size() > 0) {
                        for (Class<?> iface : ifaces) {
                            desc.addInterfaceName(iface.getName());
                        }
                    }
                    desc.addInterfaceName(comp.getBeanClass().getName());
                    components.add(desc);
                }
            }
            Collections.sort(components);
        }
        return components;
    }
}
