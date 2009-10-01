package org.nuxeo.opensocial.webengine.gadgets;

import java.net.URL;

import javax.ws.rs.GET;

import org.nuxeo.opensocial.container.service.ContainerServiceImpl;

public class UrlResource extends InputStreamResource{


  private String path;

  public UrlResource(String path){
    this.path=path;
  }

  @GET
  public Object getGadgetFile() throws Exception {


    URL url = ContainerServiceImpl.class.getResource(path);

    return getObject(url.openStream(),path);


  }
}
