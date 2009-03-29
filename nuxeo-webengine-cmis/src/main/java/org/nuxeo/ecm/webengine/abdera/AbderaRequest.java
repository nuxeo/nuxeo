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
 */
package org.nuxeo.ecm.webengine.abdera;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.abdera.protocol.server.Provider;
import org.apache.abdera.protocol.server.Target;
import org.apache.abdera.protocol.server.TargetType;
import org.apache.abdera.protocol.server.impl.SimpleTarget;
import org.apache.abdera.protocol.server.servlet.ServletRequestContext;
import org.nuxeo.ecm.webengine.atom.UrlResolver;
import org.nuxeo.ecm.webengine.model.WebContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@SuppressWarnings("unchecked")
public class AbderaRequest extends ServletRequestContext {

    public final static String PARAMS_KEY = "ABDERA_PARAMETERS";
    public final static String URL_RESOLVER_KEY = "ABDERA_URL_RESOLVER";
    
    protected Target target;
    protected WebContext ctx;
    protected UrlResolver urlResolver; 

    public static void setParameter(WebContext ctx, String key, String value) {
        Map<String,String> map = (Map<String,String>)ctx.getProperty(PARAMS_KEY);
        if (map == null) {
            map = new HashMap<String, String>();
            ctx.setProperty(PARAMS_KEY, map);
        }        
        map.put(key, value);                
    }    
    
    public AbderaRequest(TargetType targetType, Provider provider, WebContext ctx) {
        super (provider, ctx.getRequest());
        this.target = new SimpleTarget(targetType, this);  
        this.ctx = ctx;
        this.urlResolver = (UrlResolver)this.ctx.getProperty(URL_RESOLVER_KEY);
    }
    
    // Avoid calling the provider for the target. it will be initialized after super ctor is called
    @Override
    protected Target initTarget() {
        return null;
    }
    
    public Target getTarget() {
        return target;
    }
    
    public void setTarget(Target target) {
        this.target = target;
    }

    public void setParameter(String key, String value) {        
        setParameter(ctx, key, value);
    }
    
    @Override
    public String getParameter(String name) {
        Map<String,String> map = (Map<String,String>)ctx.getProperty(PARAMS_KEY);
        if (map == null) {
            return super.getParameter(name);
        }
        String val = map.get(name);
        if (val == null) {
            val = super.getParameter(name);
        }
        return val;
    }
    
    @Override
    public String[] getParameterNames() {
        Map<String,String> map = (Map<String,String>)ctx.getProperty(PARAMS_KEY);
        if (map == null) {
            return super.getParameterNames();
        }
        ArrayList<String> result = new ArrayList<String>();
        result.addAll(map.keySet());
        result.addAll(Arrays.asList(super.getParameterNames()));
        return result.toArray(new String[result.size()]);
    }
    
    @Override
    public List<String> getParameters(String name) {
        Map<String,String> map = (Map<String,String>)ctx.getProperty(PARAMS_KEY);
        if (map == null) {
            return super.getParameters(name);
        }
        String val = map.get(name);
        if (val != null) {
            return Collections.singletonList(val);
        }
        return super.getParameters(name);
    }

    /**
     * Delegate this to the registered resolver
     */
    @Override
    public String urlFor(Object key, Object param) {
        return urlResolver.urlFor(ctx, key, param);
    }
    
}


