

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Properties;
import java.util.List;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Launcher {

    // properties to be used only when running from command line.
    // the main class to load
    public static final String LOADER_MAIN = "org.nuxeo.runtime.loader.main";
    // the loader configuration file
    public static final String LOADER_CONFIG = "org.nuxeo.runtime.loader.configuration";
    // the loader class path
    public static final String CLASS_PATH = "org.nuxeo.runtime.loader.classpath";
    // the runtime loader class
    public static final String RUNTIME_LOADER = "org.nuxeo.runtime.loader.runtime_loader";
    public static final String RUNTIME_LOADER_METHOD = "org.nuxeo.runtime.loader.runtime_loader_method";

    /**
     *
     */
    private Launcher() {
    }

    private static String[] getClassPath(Properties properties) {
        String rawcp = properties.getProperty(CLASS_PATH, ".");
        if (rawcp == null) {
            return new String[0];
        }
        return rawcp.split(":");
    }

    private static String getRuntimeLoaderClass(Properties properties) {
        return properties.getProperty(RUNTIME_LOADER, "org.nuxeo.runtime.launcher.RuntimeLoader");
    }

    private static String getRuntimeLoaderMethod(Properties properties) {
        return properties.getProperty(RUNTIME_LOADER_METHOD, "loadRuntime");
    }

    private static void startMain(ClassLoader classLoader,
            Properties properties, String[] args)
            throws ClassNotFoundException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
        String className = properties.getProperty(LOADER_MAIN);
        if (className != null) {
            Class<?> main = classLoader.loadClass(className);
            Method method = main.getMethod("main", String[].class);
            method.invoke(null, new Object[] { args });
        } else {
            System.err.println("Nuxeo Runtime loaded but no main class specified");
        }
    }

    public static void main(String[] args) {

        String configFile;
        if (args.length == 0) {
            configFile = System.getProperty(LOADER_CONFIG);
            if (configFile == null) {
                System.err.println("Nuxeo Runtime loader configuration not specified.\n"
                        + "You must specify it either as the path to a java properties file as a main application argument,\n"
                        + " either through the system property: "
                        + LOADER_CONFIG);
                System.exit(1);
            }
        } else {
            configFile = args[0];
        }

        Properties properties = new Properties();
        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(configFile));
            properties.load(in);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            if (in != null) {
                try { in.close(); } catch (IOException ee) {}
            }
        }

        ClassLoader cl = Launcher.class.getClassLoader();

        try {
            String[] cp = getClassPath(properties);
            List<URL> urls = new ArrayList<URL>();
            for (String entry : cp) {
                File entryFile;
                if (entry.endsWith("/*")) {
                    entryFile = new File(entry.substring(0, entry.length() - 2));
                    for (File file : entryFile.listFiles()) {
                        urls.add(file.toURI().toURL());
                    }
                } else {
                    entryFile = new File(entry);
                }
                urls.add(entryFile.toURI().toURL());
            }
            cl = new URLClassLoader(urls.toArray(new URL[urls.size()]), cl);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        ClassLoader runtimeCl = null;
        try {
            Class<?> loaderClass = Class.forName(getRuntimeLoaderClass(properties), true, cl);
            Constructor<?> ctor = loaderClass.getConstructor(Properties.class);
            Object loader = ctor.newInstance(properties);
            Method method = loaderClass.getMethod(getRuntimeLoaderMethod(properties), ClassLoader.class);
            runtimeCl = (ClassLoader) method.invoke(loader, cl);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        String[] newArgs;
        if (args.length == 0) {
            newArgs = args;
        } else {
            newArgs = new String[args.length - 1];
            System.arraycopy(args, 1, newArgs, 0, newArgs.length);
        }

        try {
            startMain(runtimeCl, properties, newArgs);
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        System.exit(0);
    }

}
