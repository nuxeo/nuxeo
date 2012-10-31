/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.styleguide.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.styleguide.service.descriptors.IconDescriptor;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 5.7
 */
public class StyleGuideServiceImpl extends DefaultComponent implements
        StyleGuideService {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(StyleGuideServiceImpl.class);

    protected StyleGuideIconRegistry iconsReg;

    // Runtime Component API

    @Override
    public void activate(ComponentContext context) throws Exception {
        super.activate(context);
        iconsReg = new StyleGuideIconRegistry();
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (contribution instanceof IconDescriptor) {
            IconDescriptor icon = (IconDescriptor) contribution;
            log.info(String.format("Register icon '%s'", icon.getPath()));
            registerIcon(icon);
            log.info(String.format("Done registering icon '%s'", icon.getPath()));
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (contribution instanceof IconDescriptor) {
            IconDescriptor icon = (IconDescriptor) contribution;
            log.info(String.format("Unregister icon '%s'", icon.getPath()));
            unregisterIcon(icon);
            log.info(String.format("Done unregistering icon '%s'",
                    icon.getPath()));
        }
    }

    protected void registerIcon(IconDescriptor icon) {
        iconsReg.addContribution(icon);
    }

    protected void unregisterIcon(IconDescriptor icon) {
        iconsReg.removeContribution(icon);
    }

    // Service API

    @Override
    public Map<String, List<IconDescriptor>> getIconsByCat(
            List<String> allIconsPaths) {
        Map<String, List<IconDescriptor>> res = new HashMap<String, List<IconDescriptor>>();
        // add "unknown" cat by default
        List<IconDescriptor> unknownCat = new ArrayList<IconDescriptor>();
        res.put("unknown", unknownCat);
        if (allIconsPaths != null) {
            for (String path : allIconsPaths) {
                IconDescriptor desc = iconsReg.getIcon(path);
                if (desc == null) {
                    desc = new IconDescriptor();
                    desc.setPath(path);
                    desc.setLabel(FileUtils.getFileName(path));
                    desc.setCategories(Arrays.asList("unknown"));
                }
                if (Boolean.FALSE.equals(desc.getEnabled())) {
                    continue;
                }
                List<String> cats = desc.getCategories();
                if (cats != null) {
                    for (String cat : cats) {
                        if (!res.containsKey(cat)) {
                            List<IconDescriptor> newCat = new ArrayList<IconDescriptor>();
                            res.put(cat, newCat);
                        }
                        res.get(cat).add(desc);
                    }
                }
            }
        }
        return res;
    }

}
