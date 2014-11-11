/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.runtime.model.impl;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject
public class ServiceDescriptor implements Serializable {

    private static final long serialVersionUID = 2732085252068872368L;

    // TODO: it should be an error in XMap -> normally you should specify node
    // paths relative to the current object element,
    // so you should have serviceFactory instead of service@serviceFactory - but
    // seems it not works
    @XNode("service@serviceFactory")
    boolean isFactory;

    // service class names
    @XNodeList(value = "service/provide@interface", type = String[].class, componentType = String.class)
    String[] services;

}
