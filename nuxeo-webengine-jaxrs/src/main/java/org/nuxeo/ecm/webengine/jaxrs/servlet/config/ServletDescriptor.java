/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.webengine.jaxrs.servlet.config;

import java.util.Hashtable;
import java.util.Map;

import javax.servlet.Servlet;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.webengine.jaxrs.BundleNotFoundException;
import org.nuxeo.ecm.webengine.jaxrs.Utils;
import org.nuxeo.ecm.webengine.jaxrs.Utils.ClassRef;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("servlet")
public class ServletDescriptor {

    // the filter name if any
    @XNode("@name")
    protected String name;

    @XNode("@class")
    protected String classRef;

    /**
     * The absolute path of the servlet (including the context path)
     */
    @XNode("@path")
    protected String path;

    private ClassRef ref;

    /**
     * Must use hashtable since it extends Dictionary
     */
    @XNodeMap(value="param", key="@name", type=Hashtable.class, componentType=String.class, trim=true, nullByDefault=false)
    protected Hashtable<String, String> initParams;


    public ServletDescriptor() {
    }


    public String getPath() {
        return path;
    }

    public Map<String, String> getInitParams() {
        return initParams;
    }

    public void setInitParams(Hashtable<String, String> initParams) {
        this.initParams = initParams;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isInitialized() {
        return ref != null;
    }

    public ClassRef getClassRef() throws ClassNotFoundException, BundleNotFoundException {
        if (ref == null) {
            ref = Utils.getClassRef(classRef);
        }
        return ref;
    }

    public Servlet getServlet() throws Exception {
        return (Servlet)getClassRef().get().newInstance();
    }

}
