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

@WebModule(name="management")

@Path("/management")
@Produces(["text/html; charset=UTF-8"])
public class Main extends DefaultModule {

  protected final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
    
  /**
   * Default view
   */
  @GET
  public Object doGet() {
    return getTemplate("index.ftl");
  }

  @GET
  @Path("/services")
  public Object doGetServices() {
    return getTemplate("services.ftl");
  }
  
  public Set<ObjectName> getServices() {
      return mbeanServer.queryNames(null,null)
  }
  
  @GET
  @Path("/resources")
  public Object doGetResources() {
    return getTemplate("resources.ftl");
  }
  

  
  public Set<ObjectName> getResources() {
      return mbeanServe.queryNames(new ObjectName(null,null));
  }
  


  protected ObjectName objectName;
  protected MBeanInfo objectInfo;
  
  @GET
  @Path("/{name}")
  public Object doGetResource(@PathParam("name") String name) {
      this.objectName = new ObjectName(name);
      this.objectInfo = mbeanServer.getMBeanInfo(objectName);
      return getTemplate("resource.ftl");
  }
  
  public ObjectName getObjectName() {
      return objectName;
  }
  
  public MBeanInfo getObjectInfo() {
      return objectInfo;
  }
  
  public String getObjectAttribute(MBeanAttributeInfo attributeInfo) {
      return mbeanServer.getAttribute(objectName,attributeInfo.getName()).toString();
  }
 
}

