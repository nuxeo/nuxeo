package org.nuxeo.ecm.webengine.rest.servlet.resteasy.patch;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.resteasy.PathParamIndex;
import org.resteasy.spi.ConstructorInjector;
import org.resteasy.spi.InjectorFactory;
import org.resteasy.spi.MethodInjector;
import org.resteasy.spi.PropertyInjector;
import org.resteasy.spi.ResteasyProviderFactory;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class InjectorFactoryImpl implements InjectorFactory
{
   private PathParamIndex index;
   private ResteasyProviderFactory factory;


   public InjectorFactoryImpl(PathParamIndex index, ResteasyProviderFactory factory)
   {
      this.index = index;
      this.factory = factory;
   }

   public ConstructorInjector createConstructor(Constructor constructor)
   {
      return new ConstructorInjectorImpl(constructor, index, factory);
   }

   public PropertyInjector createPropertyInjector(Class resourceClass)
   {
      return new PropertyInjectorImpl(resourceClass, index, factory);
   }

   public MethodInjector createMethodInjector(Method method)
   {
      return new MethodInjectorImpl(method, index, factory);
   }

}
