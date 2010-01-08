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

package org.nuxeo.opensocial.gadgets.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.opensocial.gadgets.service.api.GadgetDeclaration;
import org.nuxeo.opensocial.gadgets.service.api.GadgetService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class GadgetServiceImpl extends DefaultComponent implements
    GadgetService {

  private static final String URL_SEPARATOR = "/";

  private static final String HTTP_SEPARATOR = ":";

  private static final String GADGETS_PORT = "gadgets.port";
  private static final String GADGETS_HOST = "gadgets.host";
  private static final String GADGETS_PATH = "gadgets.path";

  private static final String GWTGADGETS_PORT = "gwtgadgets.port";
  private static final String GWTGADGETS_HOST = "gwtgadgets.host";
  private static final String GWTGADGETS_PATH = "gwtgadgets.path";

  private static final String HTTP = "http://";

  private static final String GADGET_XP = "gadget";

  private Map<String, GadgetDeclaration> gadgets = new HashMap<String, GadgetDeclaration>();

  private static final Log log = LogFactory.getLog(GadgetServiceImpl.class);

  @Override
  public void registerContribution(Object contribution, String extensionPoint,
      ComponentInstance contributor) throws Exception {

    if (GADGET_XP.equals(extensionPoint)) {
      registerNewGadget((GadgetDeclaration) contribution, contributor);
    }
  }

  @Override
  public void unregisterContribution(Object contribution,
      String extensionPoint, ComponentInstance contributor) throws Exception {
    if (GADGET_XP.equals(extensionPoint)) {
      unregisterNewGadget((GadgetDeclaration) contribution, contributor);
    }
  }

  private void registerNewGadget(GadgetDeclaration gadget,
      ComponentInstance contributor) {

    if (gadgets.containsKey(gadget.getName())) {
      gadgets.remove(gadget.getName());
    }

    gadget.setComponentName(contributor.getName());
    gadgets.put(gadget.getName(), gadget);
  }

  private void unregisterNewGadget(GadgetDeclaration gadget,
      ComponentInstance contributor) {
    if (gadgets.containsKey(gadget.getName())) {
      gadgets.remove(gadget.getName());
    }

  }

  public GadgetDeclaration getGadget(String name) {
    if (gadgets.containsKey(name))
      return gadgets.get(name);
    return null;
  }

  public InputStream getGadgetResource(String gadgetName, String resourcePath)
      throws IOException {
    GadgetDeclaration gadget = getGadget(gadgetName);
    URL gadgetURL;
    ComponentInstance component = Framework.getRuntime()
        .getComponentInstance(gadget.getComponentName());
    gadgetURL = component.getRuntimeContext()
        .getBundle()
        .getEntry("gadget/" + gadget.getDirectory() + "/" + resourcePath);
    if (gadgetURL != null) {
      return gadgetURL.openStream();
    } else {
      return null;
    }

  }

  public List<GadgetDeclaration> getGadgetList() {
    List<GadgetDeclaration> gadgetList = new ArrayList<GadgetDeclaration>();
    for (GadgetDeclaration gadget : gadgets.values()) {
      gadgetList.add(gadget);
    }
    return gadgetList;
  }

  public List<GadgetDeclaration> getGadgetList(String category) {
    return getGadgetList();
  }

  public HashMap<String, ArrayList<String>> getGadgetNameByCategory() {
    HashMap<String, ArrayList<String>> listByCategories = new HashMap<String, ArrayList<String>>();
    for (GadgetDeclaration gadget : gadgets.values()) {

      if (listByCategories.containsKey(gadget.getCategory())) {
        ArrayList<String> listGadget = listByCategories.get(gadget.getCategory());
        listGadget.add(gadget.getName());
      } else if (gadget.getCategory() != null) {
        ArrayList<String> listGadget = new ArrayList<String>();
        listGadget.add(gadget.getName());
        listByCategories.put(gadget.getCategory(), listGadget);
      }
    }
    return listByCategories;
  }

  public URL getGadgetDefinition(String gadgetName) {
    // TODO: FIX since it won't work on JBoss

    GadgetDeclaration gadget = getGadget(gadgetName);
    StringBuilder sb = getUrlPrefix();

    if (gadget != null) {
      sb.append(gadget.getMountPoint());
      sb.append(URL_SEPARATOR);
      sb.append(gadget.getEntryPoint());
      try {
        return new URL(sb.toString());
      } catch (MalformedURLException e) {
        e.printStackTrace();
      }
    }

    return null;
  }

  private StringBuilder getUrlPrefix() {
    StringBuilder sb = new StringBuilder();
    sb.append(HTTP);
    sb.append(Framework.getProperty(GADGETS_HOST));
    sb.append(HTTP_SEPARATOR);
    sb.append(Framework.getProperty(GADGETS_PORT));
    sb.append(Framework.getProperty(GADGETS_PATH));
    return sb;
  }

  public List<String> getGadgetCategory() {
    List<String> categories = new ArrayList<String>();
    for (GadgetDeclaration gadget : gadgets.values()) {
      if (!categories.contains(gadget.getCategory()))
        categories.add(gadget.getCategory());
    }
    return categories;
  }

  public String getIconUrl(String gadgetName) {
    StringBuilder sb = new StringBuilder(Framework.getProperty(GADGETS_PATH));
    GadgetDeclaration gadget = getGadget(gadgetName);
    sb.append(gadget.getMountPoint());
    sb.append(URL_SEPARATOR);
    sb.append(gadget.getIcon());
    return sb.toString();
  }

}
