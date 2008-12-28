package management;

import java.io.*;
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

  protected ManagementService service() {
    return Framework.getService(ManagementService.class);
  }
    
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
      return service().getServicesName();
  }
  
  @GET
  @Path("/resources")
  public Object doGetResources() {
    return getTemplate("resources.ftl");
  }
  

  
  public Set<ObjectName> getResources() {
      return service().getResourcesName();
  }
  


  protected ObjectName objectName;
  protected MBeanInfo objectInfo;
  
  @GET
  @Path("/{name}")
  public Object doGetResource(@PathParam("name") String name) {
      this.objectName = service().getObjectName(name);
      this.objectInfo = service().getObjectInfo(objectName);
      return getTemplate("resource.ftl");
  }
  
  public ObjectName getObjectName() {
      return objectName;
  }
  
  public MBeanInfo getObjectInfo() {
      return objectInfo;
  }
  
  public String getObjectAttribute(MBeanAttributeInfo attributeInfo) {
      return service().getObjectAttribute(objectName,attributeInfo).toString();
  }
 
}

