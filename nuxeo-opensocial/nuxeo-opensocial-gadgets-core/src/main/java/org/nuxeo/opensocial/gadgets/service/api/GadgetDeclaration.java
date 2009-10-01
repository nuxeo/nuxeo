package org.nuxeo.opensocial.gadgets.service.api;

import java.net.URL;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentName;
import org.osgi.framework.Bundle;

@XObject("internalGadget")
public class GadgetDeclaration {

  @XNode("@name")
  private String name;

  @XNode("@entryPoint")
  private String entryPoint;

  @XNode("mountPoint")
  private String mountPoint;

  @XNode("directory")
  private String directory;

  @XNode("category")
  private String category;

  @XNode("icon")
  private String icon;

  private Bundle bundle;

  private ComponentName componentName;

  public String getName() {
    return name;
  }

  public String getIcon() {
    return icon;
  }

  public final String getMountPoint() {
    return mountPoint;
  }

  public final String getDirectory() {
    return directory;
  }

  public final String getCategory() {
    return category;
  }

  public final void setBundle(Bundle bundle) {
    this.bundle = bundle;
  }

  public final Bundle getBundle() {
    return bundle;
  }

  public final void setComponentName(ComponentName name) {
    componentName = name;

  }

  public final ComponentName getComponentName() {
    return componentName;
  }

  public final void setEntryPoint(String entryPoint) {
    this.entryPoint = entryPoint;
  }

  public final String getEntryPoint() {
    if (entryPoint == null) {
      return getName() + ".xml";
    } else {
      return entryPoint;
    }
  }

  public URL getIconUrl() {
    try {
      return Framework.getService(GadgetService.class)
          .getIconUrl(this.name);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;

  }

}
