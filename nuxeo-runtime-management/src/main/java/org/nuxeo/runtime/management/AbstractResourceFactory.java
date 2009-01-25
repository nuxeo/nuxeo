package org.nuxeo.runtime.management;
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
 *     matic
 */


/**
 * @author matic
 * 
 */
public abstract class AbstractResourceFactory implements ResourceFactory {

    protected ResourcePublisherService service;
    protected ResourceFactoryDescriptor descriptor;

    public void configure(ResourcePublisherService service, ResourceFactoryDescriptor descriptor) {
        this.service = service;
        this.descriptor = descriptor;
    }

}
