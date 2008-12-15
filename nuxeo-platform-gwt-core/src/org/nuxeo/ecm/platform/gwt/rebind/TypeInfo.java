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

package org.nuxeo.ecm.platform.gwt.rebind;

import com.google.gwt.core.ext.typeinfo.JClassType;

/**
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TypeInfo {

    protected JClassType classType;
    protected String proxyName;
    protected String proxyQName;
    protected String proxyPackage;


    public TypeInfo(JClassType classType) {
        this.classType = classType;
        proxyName = classType.getSimpleSourceName() + "Proxy";
        proxyQName = classType.getQualifiedSourceName() + "Proxy";
        proxyPackage = classType.getPackage().getName();
    }

    /**
     * @return the classType.
     */
    public JClassType getClassType() {
        return classType;
    }
    
    /**
     * @return the proxyName.
     */
    public String getProxyName() {
        return proxyName;
    }
    
    /**
     * @return the proxyPackage.
     */
    public String getProxyPackage() {
        return proxyPackage;
    }
    
    /**
     * @return the proxyQName.
     */
    public String getProxyQName() {
        return proxyQName;
    }
    
}
