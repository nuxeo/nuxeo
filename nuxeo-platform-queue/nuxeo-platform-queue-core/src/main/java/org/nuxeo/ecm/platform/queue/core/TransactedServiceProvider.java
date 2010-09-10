package org.nuxeo.ecm.platform.queue.core;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.queue.api.Transacted;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.ServiceProvider;

/**
 * Allocate transacted invocation handlers and return proxies if
 * service is annoted with Transacted annotations.
 *
 * @author matic
 *
 */
public class TransactedServiceProvider implements ServiceProvider {

    protected static final Log log = LogFactory.getLog(TransactedServiceProvider.class);

    protected final ServiceProvider nextProvider;

    protected TransactedServiceProvider(ServiceProvider provider) {
        this.nextProvider = provider;
    }


   protected class Entry<T> {

       final Class<T> srvClass;
       final boolean isTransacted;

       protected Entry(Class<T> srvClass) {
           this.srvClass = srvClass;
           this.isTransacted = srvClass.isInterface() && hasAnnotations(srvClass);
           if (isTransacted) {
               log.info("transacted  " + srvClass.getSimpleName());
           }
       }


       protected boolean hasAnnotations(Class<?> srvClass) {
           if (srvClass.isAnnotationPresent(Transacted.class)) {
               return true;
           }
           for (Method m: srvClass.getMethods()) {
               if (m.getAnnotation(Transacted.class) != null) {
                   return true;
               }
           }
           return false;
       }

       protected T getService() {
           // do not cache srv objects because we don't know if service is adapted or not (CoreSession for instance)
            T srvObject = nextProvider != null ?
                    nextProvider.getService(srvClass) :
                    Framework.getRuntime().getService(srvClass);
            if (!isTransacted) {
                return srvObject;
            }
            return  TransactedInstanceHandler.newProxy(srvObject, srvClass);
       }
   }


   protected Map<Class<?>, Entry<?>> registry = new HashMap<Class<?>, Entry<?>>();

    @Override
    public <T> T getService(Class<T> srvClass) {
        if (!registry.containsKey(srvClass)) {
            registry.put(srvClass,  new Entry<T>(srvClass));
        }
        return srvClass.cast(registry.get(srvClass).getService());
    }


}
