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
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.opensocial.gadgets.service.api.GadgetDeclaration;
import org.nuxeo.opensocial.gadgets.service.api.GadgetService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class GadgetServiceImpl extends DefaultComponent implements
        GadgetService {

    private static final String URL_SEPARATOR = "/";

    private static final String GWTGADGETS_PORT = "gwtgadgets.port";

    private static final String GWTGADGETS_HOST = "gwtgadgets.host";

    private static final String GWTGADGETS_PATH = "gwtgadgets.path";

    private static final String GADGET_XP = "gadget";

    private static final HashMap<String, GadgetDeclaration> internalGadgets = new HashMap<String, GadgetDeclaration>();

    private static final String GADGET_DIRECTORY = "external gadget list";

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(GadgetServiceImpl.class);

    private static final String GADGET_DIR_SCHEMA = "externalgadget";

    private static final String EXTERNAL_PROP_NAME = "label";

    private static final String EXTERNAL_PROP_CATEGORY = "category";

    private static final String EXTERNAL_PROP_ENABLED = "enabled";

    private static final String EXTERNAL_PROP_URL = "url";

    private static final String EXTERNAL_PROP_ICON_URL = "iconUrl";

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
        if (internalGadgets.containsKey(gadget.getName())) {
            internalGadgets.remove(gadget.getName());
        }
        if (!gadget.getDisabled()) {
            internalGadgets.put(gadget.getName(), gadget);
        }
    }

    private void unregisterNewGadget(GadgetDeclaration gadget,
            ComponentInstance contributor) {
        if (internalGadgets.containsKey(gadget.getName())) {
            internalGadgets.remove(gadget.getName());
        }

    }

    public GadgetDeclaration getGadget(String name) {
        Map<String, GadgetDeclaration> gadgets = getInternalAndExternalGadgets();
        if (gadgets.containsKey(name))
            return gadgets.get(name);
        return null;
    }

    public InputStream getGadgetResource(String gadgetName, String resourcePath)
            throws IOException {
        return getGadget(gadgetName).getResourceAsStream(resourcePath);

    }

    public List<GadgetDeclaration> getGadgetList() {
        Map<String, GadgetDeclaration> gadgets = getInternalAndExternalGadgets();
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
        Map<String, GadgetDeclaration> gadgets = getInternalAndExternalGadgets();
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
        if (gadget==null) {
            log.warn("Unable to find gadget" + gadgetName);
            return null;
        }
        try {
            return gadget.getGadgetDefinition();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<String> getGadgetCategory() {
        Map<String, GadgetDeclaration> gadgets = getInternalAndExternalGadgets();
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

    protected Map<String, GadgetDeclaration> getInternalAndExternalGadgets() {
        HashMap<String, GadgetDeclaration> result = new HashMap<String, GadgetDeclaration>();
        for (String key : internalGadgets.keySet()) {
            result.put(key, internalGadgets.get(key));
        }
        try {
            DirectoryService dirService = Framework.getService(DirectoryService.class);
            Session session = dirService.open(GADGET_DIRECTORY);
            try {
                for (DocumentModel model : session.getEntries()) {
                    String name = (String) model.getProperty(GADGET_DIR_SCHEMA,
                            EXTERNAL_PROP_NAME);
                    String category = (String) model.getProperty(
                            GADGET_DIR_SCHEMA, EXTERNAL_PROP_CATEGORY);
                    long enabled = (Long) model.getProperty(GADGET_DIR_SCHEMA,
                            EXTERNAL_PROP_ENABLED);
                    boolean disabled = enabled != 0 ? false : true;

                    String gadgetDefinition = (String) model.getProperty(
                            GADGET_DIR_SCHEMA, EXTERNAL_PROP_URL);
                    String iconURL = (String) model.getProperty(
                            GADGET_DIR_SCHEMA, EXTERNAL_PROP_ICON_URL);
                    ExternalGadgetDescriptor desc = new ExternalGadgetDescriptor(
                            category, disabled, new URL(gadgetDefinition),
                            iconURL, name);
                    if (!desc.getDisabled()) {
                        result.put(desc.getName(), desc);
                    }
                }
            } finally {
                if (session!=null) {
                    session.close();
                }
            }

        } catch (Exception e) {
            log.error("Unable to read external gadget directory!", e);

        }
        return result;
    }
}
