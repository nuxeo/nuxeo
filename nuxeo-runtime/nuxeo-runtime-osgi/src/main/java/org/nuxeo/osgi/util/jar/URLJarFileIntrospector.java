package org.nuxeo.osgi.util.jar;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

public class URLJarFileIntrospector {

    protected Method factoryGetMethod;

    protected Method factoryCloseMethod;

    protected Field jarField;

    protected Method getJarFileMethod;

    Field ucpField;

    Field lmapField;

    Field loadersField;

    Field jarFileFactoryField;

    Object factory;


    public URLJarFileIntrospector() throws URLJarFileIntrospectionError {
        try {
            ucpField = URLClassLoader.class.getDeclaredField("ucp");
            ucpField.setAccessible(true);
            Class<?> ucpClass = loadClass("sun.misc.URLClassPath");
            lmapField = ucpClass.getDeclaredField("lmap");
            lmapField.setAccessible(true);
            loadersField = ucpClass.getDeclaredField("loaders");
            loadersField.setAccessible(true);
            Class<?> jarLoaderClass = loadClass("sun.misc.URLClassPath$JarLoader");
            jarField = jarLoaderClass.getDeclaredField("jar");
            jarField.setAccessible(true);
            getJarFileMethod = jarLoaderClass.getDeclaredMethod("getJarFile",
                    new Class<?>[] { URL.class });
            getJarFileMethod.setAccessible(true);
            Class<?> jarURLConnectionClass = loadClass("sun.net.www.protocol.jar.JarURLConnection");
            jarFileFactoryField = jarURLConnectionClass.getDeclaredField("factory");
            jarFileFactoryField.setAccessible(true);
            factory = jarFileFactoryField.get(null);
            Class<?> factoryClass = loadClass("sun.net.www.protocol.jar.JarFileFactory");
            factoryGetMethod = factoryClass.getMethod("get",
                    new Class<?>[] { URL.class });
            factoryGetMethod.setAccessible(true);
            factoryCloseMethod = factoryClass.getMethod("close",
                    new Class<?>[] { JarFile.class });
            factoryCloseMethod.setAccessible(true);
        } catch (NoSuchFieldException | SecurityException
                | ClassNotFoundException | NoSuchMethodException
                | IllegalArgumentException | IllegalAccessException cause) {
            throw new URLJarFileIntrospectionError("Cannot introspect url class loader jar files", cause);
        }
    }

    protected Object fetchFactory() throws URLJarFileIntrospectionError {
        try {
            return jarFileFactoryField.get(null);
        } catch (IllegalArgumentException | IllegalAccessException cause) {
            throw new URLJarFileIntrospectionError("Cannot access to factory", cause);
        }
    }

    protected static Class<?> loadClass(String name)
            throws ClassNotFoundException {
        return URLJarFileIntrospector.class.getClassLoader().loadClass(name);
    }

    public JarFileCloser newJarFileCloser(ClassLoader loader) throws URLJarFileIntrospectionError {
        return new URLJarFileCloser(this, loader);
    }

    protected URLClassLoaderCloser newURLClassLoaderCloser(URLClassLoader loader) throws URLJarFileIntrospectionError {
        try {
            Object ucp = ucpField.get(loader);
            Map<?, ?> index = (Map<?, ?>) lmapField.get(ucp);
            List<?> loaders = (List<?>) loadersField.get(ucp);
            return new URLClassLoaderCloser(this, index, loaders);
        } catch (IllegalArgumentException | IllegalAccessException cause) {
            throw new URLJarFileIntrospectionError("Cannot unwrap url class loader fields", cause);
        }
    }


  public void close(URL location) throws IOException {
        JarFile jar = null;
        try {
            jar = (JarFile) factoryGetMethod.invoke(factory,
                    new Object[] { location });
            factoryCloseMethod.invoke(factory, jar);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(
                    "Cannot use reflection on jar file factory", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(
                    "Cannot use reflection on jar file factory", e);
        }
        jar.close();
    }
}
