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
 *     matic
 */
package org.apache.abdera.ext.cmis;

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.abdera.Abdera;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.ExtensibleElementWrapper;
import org.apache.abdera.xpath.XPath;

/**
 * @author matic
 *
 */
public class CmisRepositoryInfo extends ExtensibleElementWrapper {
    
    public CmisRepositoryInfo(Element internal) {
        super(internal);
    }

    public CmisRepositoryInfo(Factory factory, QName qname) {
        super(factory, qname);
    }

    @SuppressWarnings({"unchecked"})
    public String getValue(String propertyName) {
        // TODO: xpath is probably less efficient than DOM navigation
        XPath xpath = Abdera.getInstance().getXPath();
        Map<String, String> ns = xpath.getDefaultNamespaces();
        ns.put("c", CmisConstants.CMIS_NS);
        String req = String.format("c:%s", propertyName);
        List<Element> elements = (List<Element>) xpath.evaluate(req, this, ns);
        if (elements.size() == 0) {
            return null;
        }
        return elements.get(0).getText();
    }
    
    public String getRepositoryId() {
        return getValue("repositoryId");
    }

}
