/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
