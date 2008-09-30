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

package org.nuxeo.ecm.webengine.rest.servlet.resteasy.patch;

import java.lang.reflect.Method;
import java.util.Set;

import javax.ws.rs.Path;

import org.resteasy.InjectorFactoryImpl;
import org.resteasy.PathParamIndex;
import org.resteasy.ResourceMethod;
import org.resteasy.plugins.server.resourcefactory.POJOResourceFactory;
import org.resteasy.plugins.server.resourcefactory.SingletonResource;
import org.resteasy.specimpl.UriBuilderImpl;
import org.resteasy.spi.InjectorFactory;
import org.resteasy.spi.ResourceFactory;
import org.resteasy.spi.ResteasyProviderFactory;
import org.resteasy.util.IsHttpMethod;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ResourceMethodRegistry extends org.resteasy.ResourceMethodRegistry {

    public ResourceMethodRegistry(ResteasyProviderFactory factory) {
        super (factory);
    }


    public void addSingletonResource(Object singleton, String clazzPath, boolean encoded, boolean limited) {
        this.addResourceFactory(new SingletonResource(singleton), singleton.getClass(), clazzPath, encoded, limited);
    }

    public void addPojoResource(Class<?> clazz, String clazzPath, boolean encoded, boolean limited) {
        this.addResourceFactory(new POJOResourceFactory(clazz), clazz, clazzPath, encoded, limited);
    }

    public void addResourceFactory(ResourceFactory ref, Class<?> clazz, String clazzPath, boolean encoded, boolean limited) {
        this.addResourceFactory(ref, null, clazz, clazzPath, encoded, limited, 0);
    }


    /**
     * Register a root resource that is not annotated with {@link Path}
     * The path information is passed through arguments.
     * @param ref
     * @param base
     * @param clazz
     * @param clazzPath
     * @param encoded
     * @param limited
     */
    protected void addResourceFactory(ResourceFactory ref, String base, Class<?> clazz, String clazzPath, boolean encoded, boolean limited, int offset)
    {
       if (ref != null) ref.registered(new InjectorFactoryImpl(null, providerFactory));
       for (Method method : clazz.getMethods())
       {
          Path path = method.getAnnotation(Path.class);
          Set<String> httpMethods = IsHttpMethod.getHttpMethods(method);
          if (path == null && httpMethods == null) continue;

          UriBuilderImpl builder = new UriBuilderImpl();
          builder.encode(encoded);
          builder.setPath(base);
          builder.path(clazzPath);
//          if (clazz.isAnnotationPresent(Path.class))
//          {
//             builder.path(clazz);
//             limited = clazz.getAnnotation(Path.class).limited();
//          }
          if (path != null)
          {
             if (limited == false)
                throw new RuntimeException("It is illegal to have @Path.limited() == false on your class then use a @Path on a method too");
             builder.path(method);
             limited = path.limited();
          }
          String pathExpression = builder.getPath();
          if (pathExpression == null) pathExpression = "";

          PathParamIndex index = new PathParamIndex(pathExpression, offset, !limited);
          InjectorFactory injectorFactory = new InjectorFactoryImpl(index, providerFactory);
          if (pathExpression.startsWith("/")) pathExpression = pathExpression.substring(1);
          String[] paths = pathExpression.split("/");
          if (httpMethods == null)
          {
             ResourceLocator locator = new ResourceLocator(ref, injectorFactory, providerFactory, method, index, limited);
             root.addChild(paths, 0, locator);
          }
          else
          {
             ResourceMethod invoker = new ResourceMethod(clazz, method, injectorFactory, ref, providerFactory, httpMethods, index);
             root.addChild(paths, 0, invoker, !limited);
          }
          size++;

       }
    }


    /**
     * Override to change {@link InjectorFactoryImpl} with
     */
    public void addResourceFactory(ResourceFactory ref, String base, Class<?> clazz, int offset, boolean limited)
    {
       if (ref != null) ref.registered(new InjectorFactoryImpl(null, providerFactory));
       for (Method method : clazz.getMethods())
       {
          Path path = method.getAnnotation(Path.class);
          Set<String> httpMethods = IsHttpMethod.getHttpMethods(method);
          if (path == null && httpMethods == null) continue;

          UriBuilderImpl builder = new UriBuilderImpl();
          builder.setPath(base);
          if (clazz.isAnnotationPresent(Path.class))
          {
             builder.path(clazz);
             limited = clazz.getAnnotation(Path.class).limited();
          }
          if (path != null)
          {
// TODO buggy code: it is rejecting sub-resources with Path annotations when parent resource have limited=false
// there is nowhere in the specification saying that ... this is removing the utility of limited=false flag
//             if (limited == false)
//                throw new RuntimeException("It is illegal to have @Path.limited() == false on your class then use a @Path on a method too");
             builder.path(method);
             limited = path.limited();
          }
          String pathExpression = builder.getPath();
          if (pathExpression == null) pathExpression = "";

          PathParamIndex index = new PathParamIndex(pathExpression, offset, !limited);
          InjectorFactory injectorFactory = new InjectorFactoryImpl(index, providerFactory);
          if (pathExpression.startsWith("/")) pathExpression = pathExpression.substring(1);
          String[] paths = pathExpression.split("/");
          if (httpMethods == null)
          {
             ResourceLocator locator = new ResourceLocator(ref, injectorFactory, providerFactory, method, index, limited);
             root.addChild(paths, 0, locator);
          }
          else
          {
             ResourceMethod invoker = new ResourceMethod(clazz, method, injectorFactory, ref, providerFactory, httpMethods, index);
             root.addChild(paths, 0, invoker, !limited);
          }
          size++;

       }
    }

}
