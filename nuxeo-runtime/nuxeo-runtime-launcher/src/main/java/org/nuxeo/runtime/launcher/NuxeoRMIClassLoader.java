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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.launcher;
import java.net.MalformedURLException;
import java.rmi.server.RMIClassLoader;
import java.rmi.server.RMIClassLoaderSpi;

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

/**
 * Copied from org.jboss.system.JBossRMIClassLoader and modified getClassAnnotation
 * to avoid delegating to default loader since it has a bug.
 *
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class NuxeoRMIClassLoader
   extends RMIClassLoaderSpi
{
   // Attributes ----------------------------------------------------

   /**
    * The JVM implementation (we delegate most work to it)
    */
   RMIClassLoaderSpi delegate = RMIClassLoader.getDefaultProviderInstance();

   // Constructors --------------------------------------------------

   /**
    * Required constructor
    */
   public NuxeoRMIClassLoader()
   {
   }

   // RMIClassLoaderSpi Implementation ------------------------------

   /**
    * Ignore the JVM, use the thread context classloader for proxy caching
    */
   public Class<?> loadProxyClass(String codebase, String[] interfaces, ClassLoader ignored)
      throws MalformedURLException, ClassNotFoundException
   {
      return delegate.loadProxyClass(codebase, interfaces, Thread.currentThread().getContextClassLoader());
   }

   /**
    * Just delegate
    */
   public Class<?> loadClass(String codebase, String name, ClassLoader ignored)
      throws MalformedURLException, ClassNotFoundException
   {
      return delegate.loadClass(codebase, name, Thread.currentThread().getContextClassLoader());
   }

   /**
    * Just delegate
    */
   public ClassLoader getClassLoader(String codebase)
      throws MalformedURLException
   {
      return delegate.getClassLoader(codebase);
   }

   /**
    * Try to delegate an default to the java.rmi.server.codebase on any
    * failure.
    */
   public String getClassAnnotation(Class<?> cl)
   {
       return System.getProperty("java.rmi.server.codebase");
   }
}
