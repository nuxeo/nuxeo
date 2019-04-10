/*
 * (C) Copyright 2002-2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.importer.xml.parser;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor that is used to define how DocumenModel should be created from XML input
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@XObject("docConfig")
public class DocConfigDescriptor {

    @XNode("@tagName")
    protected String tagName;

    @XNode("docType")
    protected String docType;

    @XNode("parent")
    protected String parent;

    @XNode("name")
    protected String name;

    @XNode("postCreationAutomationChain")
    protected String automationChain;

    @XNode("@updateExistingDocuments")
    protected boolean update = false;

    
    public DocConfigDescriptor() {
    }

    public DocConfigDescriptor(String tagName, String docType, String parent, String name) {
        this.tagName = tagName;
        this.docType = docType;
        this.parent = parent;
        this.name = name;
    }

    public String getTagName() {
        return tagName;
    }

    public String getDocType() {
        return docType;
    }

    public String getParent() {
        return parent;
    }

    public String getName() {
        return name;
    }

    public boolean getUpdate() {
        return update;
    }

    public String getAutomationChain() {
        return automationChain;
    }

    @Override
    public String toString() {
        String msg = "\nDocConfig:\n\tTag Name: %s\n\tDocType %s\n\tParent: %s\n\tName: %s\n\tOverwrite: %s\n";
        return String.format(msg, tagName, docType, parent, name, update);
    }

}
