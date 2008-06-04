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

package org.nuxeo.runtime.binding;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.LinkRef;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

/**
 * Binds the Nuxeo bean services using the canonical binding name.
 * <p>
 * The binding name is of the form:
 * <code>nxservice/interfaceName/remote</code>
 * and
 * <code>nxservice/interfaceName/local</code>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ServiceBindings implements BundleListener {

    public final static Log log = LogFactory.getLog(ServiceBindings.class);

    protected BundleContext bundleContext;
    private InitialContext jndiContext;


    public ServiceBindings(BundleContext ctx) throws NamingException {
        this (ctx, new InitialContext());
    }

    public ServiceBindings(BundleContext bundleContext, InitialContext jndContext) {
        this.bundleContext = bundleContext;
        this.bundleContext.addBundleListener(this);
    }

    /**
     * Lazy get the initial context.
     * The JNDI service may be started after this one so we need to lazy get the initial context.
     * This can be solved by splitting the runtime in 2:  core and server
     * @return
     */
    public InitialContext getInitialContext() throws NamingException {
        if (jndiContext == null) {
            jndiContext = new InitialContext();
        }
        return jndiContext;
    }

    public void createAlias(String fromName, String aliasName) throws NamingException {
        createAlias(getInitialContext(), fromName, aliasName);
    }

    public void removeAlias(String aliasName) throws NamingException {
        removeAlias(getInitialContext(), aliasName);
    }

    public void destroy() {
        bundleContext.removeBundleListener(this);
    }


    public void bundleChanged(BundleEvent event) {
        try {
            switch(event.getType()) {
            case BundleEvent.STARTED:
                Properties properties = loadBindings(event.getBundle());
                if (properties != null) {
                    for (Map.Entry<Object,Object> entry : properties.entrySet()) {
                        String itf = entry.getKey().toString();
                        String impl = entry.getValue().toString();
                        createAlias(getLocalName(impl), createLocalJndiName(itf));
                        createAlias(getRemoteName(impl), createRemoteJndiName(itf));
                    }
                }
                break;
            case BundleEvent.STOPPED:
                properties = loadBindings(event.getBundle());
                if (properties != null) {
                    for (Map.Entry<Object,Object> entry : properties.entrySet()) {
                        String itf = entry.getKey().toString();
                        removeAlias(createLocalJndiName(itf));
                        removeAlias(createRemoteJndiName(itf));
                    }
                }
                break;
            }
        } catch (Exception e) {
            log.error("Failed to process bundle: "+event.getBundle().getSymbolicName(), e);
        }
    }

    protected String createLocalJndiName(String serviceInterface) {
        return "nxservice/"+serviceInterface+"/local";
    }

    protected String createRemoteJndiName(String serviceInterface) {
        return "nxservice/"+serviceInterface+"/remote";
    }

    public Name getLocalServiceName(Class<?> itf) {
        return new JndiName("nxservice", itf.getName(), "local");
    }

    public Name getRemoteServiceName(Class<?> itf) {
        return new JndiName("nxservice", itf.getName(), "remote");
    }

    public Name getLocalServiceName(String name) {
        return new JndiName("nxservice", name, "local");
    }

    public Name getRemoteServiceName(String name) {
        return new JndiName("nxservice", name, "remote");
    }


    protected String getRemoteName(String beanClass) {
        String name = null;
        int p = beanClass.lastIndexOf('.');
        if (p > -1) {
            name = beanClass.substring(p+1);
        } else {
            name = beanClass;
        }
        return "nuxeo/"+name+"/remote";
    }

    protected String getLocalName(String beanClass) {
        String name = null;
        int p = beanClass.lastIndexOf('.');
        if (p > -1) {
            name = beanClass.substring(p+1);
        } else {
            name = beanClass;
        }
        return "nuxeo/"+name+"/local";
    }

    protected Properties loadBindings(Bundle bundle) throws IOException {
        URL url = bundle.getEntry("OSGI-INF/service.bindings");
        if (url != null) {
            InputStream in = url.openStream();
            try {
                Properties properties = new Properties();
                properties.load(in);
                return properties;
            } finally {
                in.close();
            }
        }
        return null;
    }

    public static void createAlias(InitialContext ctx, String existingName, String aliasName) throws NamingException {
        LinkRef link = new LinkRef(existingName);
        Context aliasCtx = ctx;
        Name name = ctx.getNameParser("").parse(aliasName);
        int len = name.size()-1;
        String atom = name.get(len);
        for(int i = 0; i < len; i ++) {
           String comp = name.get(i);
           try {
              aliasCtx = (Context) aliasCtx.lookup(comp);
           } catch(NameNotFoundException e) {
              aliasCtx = aliasCtx.createSubcontext(comp);
           }
        }
        aliasCtx.rebind(atom, link);

        if (log.isDebugEnabled()) {
            log.debug("Created JNDI link [" + aliasName + "] pointing to ["+existingName+"]");
        }
    }

    public static void removeAlias(InitialContext context, String aliasName) throws NamingException {
        context.unbind(aliasName);
    }

}
