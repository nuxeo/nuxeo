/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.restAPI.service;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.restlet.Restlet;

/**
 * Descriptor for a Restlet
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
@XObject(value = "restletPlugin")
public class RestletPluginDescriptor {

    @XNode("@name")
    private String name;

    @XNode("@enabled")
    private Boolean enabled = Boolean.TRUE;

    @XNodeList(value = "urlPatterns/urlPattern", type = ArrayList.class, componentType = String.class)
    private List<String> urlPatterns = new ArrayList<String>();

    @XNode("@useSeam")
    private boolean useSeam;

    @XNode("@useConversation")
    private boolean useConversation;

    @XNode("@class")
    private Class<Restlet> className;

    @XNode("@isSingleton")
    private boolean isSingleton = Boolean.FALSE;

    public Class<Restlet> getClassName() {
        return className;
    }

    public void setClassName(Class<Restlet> className) {
        this.className = className;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getUrlPatterns() {
        return urlPatterns;
    }

    public void setUrlPatterns(List<String> urlPatterns) {
        this.urlPatterns = urlPatterns;
    }

    public boolean getUseSeam() {
        return useSeam;
    }

    public void setUseSeam(boolean useSeam) {
        this.useSeam = useSeam;
    }

    public boolean getUseConversation() {
        return useConversation;
    }

    public void setUseConversation(boolean useConversation) {
        this.useConversation = useConversation;
    }

    public boolean isSingleton() {
        return isSingleton;
    }

    public void setIsSingleton(boolean isSingleton) {
        this.isSingleton = isSingleton;
    }

}
