/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.model.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.webengine.ResourceBinding;
import org.nuxeo.ecm.webengine.model.ErrorHandler;
import org.nuxeo.ecm.webengine.model.LinkDescriptor;
import org.nuxeo.ecm.webengine.model.LinkProvider;
import org.nuxeo.ecm.webengine.model.Validator;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("module")
public class ModuleConfiguration implements Cloneable {

    private static final Log log = LogFactory.getLog(ModuleConfiguration.class);

    @XNode("@path")
    protected String path;

    @XNode("@root-type")
    protected String rootType;

    @XNode("@extends")
    public String base;

    @XNode("title")
    public String title;
        
    @XNode("icon")
    public String icon;
    
    @XNode("link-provider")
    public Class<LinkProvider> linkProvider;
    
    @XNode("error-handler")
    public Class<ErrorHandler> errorHandler;
    
    
    /**
     * The module directory.
     * Must be set by the client before registering the descriptor.
     */
    @XNode("home")
    public File directory;

    @XNodeList(value="links/link", type=ArrayList.class, componentType=LinkDescriptor.class, nullByDefault=true)
    public List<LinkDescriptor> links;

    @XNodeMap(value="validators/validator", key="@type", type=HashedMap.class, componentType=Class.class, nullByDefault=true )
    public void setValidators(Map<String, Class<Validator>> m) {
        if (m != null) {
            validators = new HashMap<String, Validator>();
            for (Map.Entry<String, Class<Validator>> entry : m.entrySet()) {
                try {
                    validators.put(entry.getKey(), entry.getValue().newInstance());
                } catch (Exception e) {
                    log.error(e);
                }
            }
        }
    }
    public Map<String, Validator> validators;

    @XNodeList(value="resources/resource", type=ArrayList.class, componentType=ResourceBinding.class, nullByDefault=true)
    public List<ResourceBinding> resources;

    @XNode("templateFileExt")
    public String templateFileExt = "ftl";

    @XNodeList(value="media-types/media-type", type=MediaTypeRef[].class, componentType=MediaTypeRef.class, nullByDefault=true)
    public MediaTypeRef[] mediatTypeRefs;

    public String getTitle() {
        return title;
    }
    
    public String getIcon() {
        return icon;
    }
    
    public Class<LinkProvider> getLinkProviderClass() {
        return linkProvider;
    }
    
    public Class<ErrorHandler> getErrorHandlerClass() {
        return errorHandler;
    }

}
