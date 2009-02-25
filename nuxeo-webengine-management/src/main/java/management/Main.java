/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package management;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ResourcePublisher;
import org.nuxeo.runtime.management.ServerLocator;

@WebObject(type = "management")
@Produces("text/xml; charset=UTF-8")
public class Main extends ModuleRoot {

    protected final ResourcePublisher publisher = Framework.getLocalService(ResourcePublisher.class);

    protected final ServerLocator locator = Framework.getLocalService(ServerLocator.class);

    /**
     * Default view
     */
    @GET
    @Produces("text/xml;charset=UTF-8")
    public Object doGetShortNamesXML() {
        return getTemplate("list-shortcuts.xml.ftl").arg("shortcuts",
                publisher.getShortcutsName());
    }

    @GET
    @Path("/{name}")
    public Object doGetListAttributes(@PathParam("name") String name)
            throws Exception {
        ObjectName objectName = publisher.lookupName(name);
        MBeanServer mbeanServer = locator.lookupServer(objectName);
        MBeanInfo objectInfo = mbeanServer.getMBeanInfo(objectName);
        return getTemplate("list-attributes.xml.ftl")
                .arg("name", name)
                .arg("qualifiedName", objectName.toString())
                .arg("objectName", objectName)
                .arg("objectInfo", objectInfo);
    }

    @GET
    @Path("/{resourceName}/{attributeName}")
    @Produces("text/xml;charset=UTF-8")
    public Object doGetAttributeXml(
            @PathParam("resourceName") String resourceName,
            @PathParam("attributeName") String attributeName) throws Exception {
        return doGetAttribute(resourceName, attributeName, "attribute.xml.ftl");
    }

    protected Object doGetAttribute(String resourceName, String attributeName,
            String templateName) throws Exception {
        ObjectName objectName = publisher.lookupName(resourceName);
        MBeanServer mbeanServer = locator.lookupServer(objectName);
        Object attributeValue = mbeanServer.getAttribute(objectName,
                attributeName);
        return getTemplate(templateName)
                .arg("resource", resourceName)
                .arg("name", attributeName)
                .arg("value", attributeValue);
    }

    public String getObjectAttribute(ObjectName objectName,
            MBeanAttributeInfo attributeInfo) throws Exception {
        MBeanServer mbeanServer = locator.lookupServer(objectName);
        return mbeanServer.getAttribute(objectName, attributeInfo.getName()).toString();
    }

    public String getQualifiedName(String name) {
        return publisher.lookupName(name).toString();
    }

    public String getPath(String name) {
        return ctx.getUrlPath() + "/" + name;
    }

}
