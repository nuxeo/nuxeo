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
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class GadgetServiceImpl extends DefaultComponent implements
        GadgetService {

    private static final String URL_SEPARATOR = "/";

    private static final String GWTGADGETS_PORT = "gwtgadgets.port";

    private static final String GWTGADGETS_HOST = "gwtgadgets.host";

    private static final String GWTGADGETS_PATH = "gwtgadgets.path";

    private static final String GADGET_XP = "gadget";

    private final Map<String, GadgetDeclaration> gadgets = new HashMap<String, GadgetDeclaration>();

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(GadgetServiceImpl.class);

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {

        if (GADGET_XP.equals(extensionPoint)) {
            InternalGadgetDescriptor gadget = (InternalGadgetDescriptor) contribution;
            gadget.setComponentName(contributor.getName());

            registerNewGadget(gadget);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (GADGET_XP.equals(extensionPoint)) {
            InternalGadgetDescriptor gadget = (InternalGadgetDescriptor) contribution;

            unregisterNewGadget(gadget, contributor);
        }
    }

    /*
     * This is public primarily for testing. this is not exposed by the api.
     */
    public void registerNewGadget(GadgetDeclaration gadget) {

        if (gadgets.containsKey(gadget.getName())) {
            gadgets.remove(gadget.getName());
        }
        if (!gadget.getDisabled()) {
            gadgets.put(gadget.getName(), gadget);
        }
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
        return getGadget(gadgetName).getResourceAsStream(resourcePath);

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

        try {
            return gadget.getGadgetDefinition();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<String> getGadgetCategory() {
        List<String> categories = new ArrayList<String>();
        for (GadgetDeclaration gadget : gadgets.values()) {
            if (!categories.contains(gadget.getCategory()))
                categories.add(gadget.getCategory());
        }
        return categories;
    }

    public GadgetServiceImpl() {
    }

    public String getIconUrl(String gadgetName) {
        return getGadget(gadgetName).getIconUrl();
    }
}
