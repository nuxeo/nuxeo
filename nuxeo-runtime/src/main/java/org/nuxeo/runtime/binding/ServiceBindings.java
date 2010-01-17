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

import java.io.File;
import java.io.FileInputStream;
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
import org.nuxeo.common.Environment;
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
 */
public class ServiceBindings implements BundleListener {

    public static final Log log = LogFactory.getLog(ServiceBindings.class);

    // static bindings are used to force the binding to an explicit implementation
    // and it is binding the interface only if it is declared by a bundle in the system
    protected final Properties staticBindings;
    protected final BundleContext bundleContext;

    private InitialContext jndiContext;

    // the bindings that were done: itf ->  impl
    // this is useful to track duplicated bindings
    private final Properties bindings;

    public ServiceBindings(BundleContext ctx) throws NamingException {
        this(ctx, new InitialContext());
    }

    public ServiceBindings(BundleContext bundleContext, InitialContext jndContext) {
        this.bundleContext = bundleContext;
        this.bundleContext.addBundleListener(this);
        staticBindings = new Properties();
        bindings = new Properties();
        loadStaticBindings();
    }

    /**
     * Lazily gets the initial context.
     * <p>
     * The JNDI service may be started after this one so we need to lazy get the initial context.
     * This can be solved by splitting the runtime in 2:  core and server.
     */
    public InitialContext getInitialContext() throws NamingException {
        if (jndiContext == null) {
            jndiContext = new InitialContext();
        }
        return jndiContext;
    }

    public void createServiceAliases(String itf, String impl) throws NamingException {
        createAlias(getLocalName(impl), createLocalJndiName(itf));
        createAlias(getRemoteName(impl), createRemoteJndiName(itf));
        bindings.put(itf, impl);
    }

    public void removeServiceAliases(String itf, String impl) throws NamingException {
        removeAlias(createLocalJndiName(itf));
        removeAlias(createRemoteJndiName(itf));
        bindings.remove(itf);
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

    /**
     * Static bindings can be used to override bindings deployed from JARs.
     * <p>
     * The static bindings should be put in the configuration directory in the
     * file <code>service.bindings</code>.
     */
    protected void loadStaticBindings() {
        Environment env = Environment.getDefault();
        if (env == null) {
            log.error("No Environment found. Unable to create service bindings");
            return;
        }
        File config = Environment.getDefault().getConfig();
        if (config != null) {
            File cfg = new File(config, "service.bindings");
            try {
                staticBindings.load(new FileInputStream(cfg));
            } catch (Exception e) {
                // do nothing
            }
        }
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
                        if (log.isDebugEnabled()) {
                            String  alreadyImpl = bindings.getProperty(itf);
                            if (alreadyImpl != null) {
                                log.warn("Overriding existing service alias for ["+itf +" -> "+ alreadyImpl+"] by "+impl);
                            }
                        }
                        // use preferentially the static bindings
                        String staticImpl = staticBindings.getProperty(itf);
                        if (staticImpl != null) {
                            log.debug("Using static binding: "+itf +" -> "+staticImpl
                                    +". Overriding default implementation: "+impl);
                            impl = staticImpl;
                        }
                        createServiceAliases(itf, impl);
                    }
                }
                break;
            case BundleEvent.STOPPED:
                properties = loadBindings(event.getBundle());
                if (properties != null) {
                    for (Map.Entry<Object,Object> entry : properties.entrySet()) {
                        String itf = entry.getKey().toString();
                        String impl = entry.getValue().toString();
                        removeServiceAliases(itf, impl);
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

    /**
     * TODO XXX this method works only on jboss.
     */
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

    /**
     * TODO XXX this method is working only on jboss.
     */
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

    public static void createAlias(InitialContext ctx, String existingName,
            String aliasName) throws NamingException {
        LinkRef link = new LinkRef(existingName);
        Context aliasCtx = ctx;
        Name name = ctx.getNameParser("").parse(aliasName);
        int len = name.size() - 1;
        String atom = name.get(len);
        for (int i = 0; i < len; i++) {
            String comp = name.get(i);
            try {
                aliasCtx = (Context) aliasCtx.lookup(comp);
            } catch (NameNotFoundException e) {
                aliasCtx = aliasCtx.createSubcontext(comp);
            }
        }

        aliasCtx.rebind(atom, link);

        if (log.isDebugEnabled()) {
            log.debug("Created JNDI link [" + aliasName + "] pointing to ["
                    + existingName + "]");
        }
    }

    public static void removeAlias(InitialContext context, String aliasName) throws NamingException {
        context.unbind(aliasName);
    }

}
