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
 * $Id: TaskModuleGenerator.java 25373 2007-09-25 08:03:06Z sfermigier $
 */

package org.nuxeo.ecm.platform.syndication.workflow;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jdom.Element;
import org.jdom.Namespace;

import com.sun.syndication.feed.module.Module;
import com.sun.syndication.io.ModuleGenerator;
import com.sun.syndication.io.impl.DateParser;

public class TaskModuleGenerator implements ModuleGenerator {

    private static final Namespace TASK_NS = Namespace.getNamespace("task",
            TaskModule.URI);

    private static final Set<Namespace> NAMESPACES;

    static {
        Set<Namespace> nss = new HashSet<Namespace>();
        nss.add(TASK_NS);
        NAMESPACES = Collections.unmodifiableSet(nss);
    }

    public String getNamespaceUri() {
        return TaskModule.URI;
    }

    public Set<Namespace> getNamespaces() {
        return NAMESPACES;
    }

    public void generate(Module module, Element element) {
        // this is not necessary, it is done to avoid the namespace definition
        // in every item.
        Element root = element;
        while (root.getParent() != null && root.getParent() instanceof Element) {
            root = (Element) element.getParent();
        }
        root.addNamespaceDeclaration(TASK_NS);

        TaskModule tm = (TaskModule) module;
        if (tm.getDueDate() != null) {
            element.addContent(generateSimpleElement("dueDate",
                    DateParser.formatW3CDateTime(tm.getDueDate())));
        }
        if (tm.getStartDate() != null) {
            element.addContent(generateSimpleElement("startDate",
                    DateParser.formatW3CDateTime(tm.getStartDate())));
        }
        if (tm.getDirective() != null) {
            element.addContent(generateSimpleElement("directive",
                    tm.getDirective()));
        }
        if (tm.getName() != null) {
            element.addContent(generateSimpleElement("name", tm.getName()));
        }
        if (tm.getDescription() != null) {
            element.addContent(generateSimpleElement("description",
                    tm.getDescription()));
        }
        if (tm.getComment() != null) {
            element.addContent(generateSimpleElement("comment", tm.getComment()));
        }
    }

    protected static Element generateSimpleElement(String name, String value) {
        Element element = new Element(name, TASK_NS);
        element.addContent(value);
        return element;
    }

}
