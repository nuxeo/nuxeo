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

package org.nuxeo.ecm.webengine.server.resteasy.registry;

import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.InjectorFactory;
import org.jboss.resteasy.spi.ResourceFactory;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.loader.ClassProxy;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ReloadablePerRequestFactory implements ResourceFactory {

    protected ClassProxy classp;
    

    public ReloadablePerRequestFactory(ClassProxy classp) {
        this.classp = classp;
    }
    
    public Object createResource(HttpRequest request, HttpResponse response,
            InjectorFactory factory) {
        try {
            return classp.get().newInstance();
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    public Class<?> getScannableClass() {
        return classp.get();
    }

    public void registered(InjectorFactory factory) {
        
    }

    public void requestFinished(HttpRequest request, HttpResponse response,
            Object resource) {
       
    }

    public void unregistered() {

    }

}
