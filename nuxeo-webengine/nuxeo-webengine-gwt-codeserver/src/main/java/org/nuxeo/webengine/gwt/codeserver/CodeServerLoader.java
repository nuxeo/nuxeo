package org.nuxeo.webengine.gwt.codeserver;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CodeServerLoader extends URLClassLoader {

    CodeServerLoader(URL[] jars)  {
        super(jars , CodeServerLoader.class.getClassLoader());
    }

    @Override
    protected java.lang.Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> clazz = findLoadedClass(name);
        if (clazz != null) {
            return clazz;
        }

        if (name.equals(CodeServerLauncher.class.getName())) {
            return getParent().loadClass(name);
        }

        if (name.equals(CodeServerWrapper.class.getName())) {
            try {
                return reloadClass(CodeServerWrapper.class);
            } catch (URISyntaxException | IOException cause) {
                throw new ClassNotFoundException("Cannot reload wrapper in gwt dev class loader", cause);
            }
        }
        try {
            return ClassLoader.getSystemClassLoader().loadClass(name);
        } catch (ClassNotFoundException cause) {
            ;
        }
        synchronized (getClassLoadingLock(name)) {
            clazz = findClass(name);
            if (clazz != null) {
                return clazz;
            }
        }
        throw new ClassNotFoundException("Cannot find " + name + " in gwt class loader");
    };

    Class<?> reloadClass(Class<?> clazz) throws URISyntaxException, IOException {
        URI location = clazz.getResource(clazz.getSimpleName().concat(".class")).toURI();
        byte[] content = Files.readAllBytes(Paths.get(location));
        return defineClass(clazz.getName(), content, 0, content.length);
    }

    CodeServerLauncher load() throws ReflectiveOperationException {
        return (CodeServerLauncher) loadClass(CodeServerWrapper.class.getName()).newInstance();
    }

}
