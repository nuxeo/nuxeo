package management;

import java.io.*;
import java.lang.management.*;
import javax.management.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.nuxeo.ecm.core.rest.*;
import org.nuxeo.ecm.webengine.model.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.ecm.webengine.model.exceptions.*;
import org.nuxeo.ecm.webengine.*;
import org.nuxeo.runtime.api.*;
import org.nuxeo.runtime.management.*;
import org.apache.commons.logging.*
import org.nuxeo.ecm.webengine.model.*;
import javax.management.*;

@WebObject(type="management")
@Produces(["text/xml; charset=UTF-8"])
public class ZooMain extends DefaultModule {
	
	protected final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
	
	protected final ResourcePublisher publisher = Framework.getService(ResourcePublisher.class);
	
	/**
	 * Default view
	 */
	@GET
	@Produces(["text/xml; charset=UTF-8"])
	public Object doGetShortNamesXML() {
		return getTemplate("list-shortcuts.xml.ftl");
	}
	
	@GET
	@Path("/{name}")
	public Object doGetListAttributes(@PathParam("name") String name) {
	    ObjectName objectName = publisher.lookupName(name);
	    MBeanInfo objectInfo = mbeanServer.getMBeanInfo(objectName);
		return getTemplate("list-attributes.xml.ftl").
		    arg("name", name).
		    arg("qualifiedName", objectName.toString()).
	        arg("objectName",objectName).
	        arg("objectInfo",objectInfo);
	}
	
	@GET
    @Path("/{resourceName}/{attributeName}")
    @Produces(["text/xml; charset=UTF-8"])
    public Object doGetAttributeXml(@PathParam("resourceName") String resourceName, @PathParam("attributeName") String attributeName) {
	    return doGetAttribute(resourceName, attributeName, "attribute.xml.ftl");
	}
	
	protected Object doGetAttribute(String resourceName, String attributeName, String templateName) {
	    ObjectName objectName = publisher.lookupName(resourceName);
        Object attributeValue = mbeanServer.getAttribute(objectName, attributeName);
        return getTemplate(templateName).
            arg("resource", resourceName).
            arg("name", attributeName ).
            arg("value", attributeValue);
	}
	
	public String getObjectAttribute(ObjectName objectName, MBeanAttributeInfo attributeInfo) {
		return mbeanServer.getAttribute(objectName,attributeInfo.getName()).toString();
	}
	
	public Set<String> getShortcutsName() {
		return publisher.getShortcutsName();
	}
	
	public String getQualifiedName(String name) {
		return publisher.lookupName(name).toString();
	}
	
	
	public String getPath(String name) {
		return ctx.getUrlPath() + "/" + name;
	}
}
