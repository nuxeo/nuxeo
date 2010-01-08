/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.gadgets.service.api;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentName;
import org.osgi.framework.Bundle;

@XObject("internalGadget")
public class GadgetDeclaration {

  @XNode("@name")
  private String name;

  // File Name of the gadget's XML
  @XNode("entryPoint")
  private String entryPoint = "";

  // URL's mount point /gadgets/{mountPoint}/{entryPoint}
  @XNode("mountPoint")
  private String mountPoint = "";

  // Directory where the gadgets files are stored in the JAR
  @XNode("directory")
  private String directory = "";

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
    if ("".equals(mountPoint)) {
      return getName();
    }
    return mountPoint;
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
    if ("".equals(entryPoint)) {
      return getName() + ".xml";
    } else {
      return entryPoint;
    }
  }

  public String getIconUrl() {
    try {
      return Framework.getService(GadgetService.class)
          .getIconUrl(this.name);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;

  }

  public String getDirectory() {
    if ("".equals(directory)) {
      return getName();
    } else {
      return directory;
    }
  }

}
