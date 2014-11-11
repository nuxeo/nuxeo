/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.ecm.platform.api.ws;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author matic
 *
 */
@XObject("loader")
public class DocumentLoaderDescriptor {

    @XNode("@name")
    public  String name;
    
    public DocumentLoader instance;
    
    @XNode("@class")
    public void setClass(Class<? extends DocumentLoader> clazz) {
        try {
            instance = clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Cannot instanciate " + clazz.getName());
        }
    }

}
