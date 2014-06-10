package org.nuxeo.osgi;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class OSGiWiring {

    protected final Map<String, Set<OSGiLoader>> contents = new HashMap<String, Set<OSGiLoader>>();

    protected final OSGiLoader loader;

    protected OSGiWiring(OSGiLoader loader) {
        this.loader = loader;
    }

    @Override
    public String toString() {
        return "OSGiWiring [loader=" + loader + ", contents=" + contents + "]";
    }

    protected void wire(String path, Set<OSGiLoader> loaders) {
        if (!contents.containsKey(path)) {
            contents.put(path, new HashSet<OSGiLoader>());
        }
        Set<OSGiLoader> set = contents.get(path);
        set.addAll(loaders);
    }

    protected void wire(String path, OSGiLoader loader) {
        if (!contents.containsKey(path)) {
            contents.put(path, new HashSet<OSGiLoader>());
        }
        Set<OSGiLoader> set = contents.get(path);
        set.add(loader);
    }

    protected void unwire(String path, Set<OSGiLoader> loaders) {
        Set<OSGiLoader> set = contents.get(path);
        if (set != null) {
            set.removeAll(loaders);
        }
    }

    protected String bundlePath(URL location) {
        URI uri;
        try {
            uri = location.toURI();
        } catch (URISyntaxException cause) {
            throw new Error("Unsupported resource location " + location, cause);
        }
        if ("jar".equals(uri.getScheme())) {
            String path = uri.getSchemeSpecificPart();
            return path.substring(path.indexOf('!')+1, path.length());
        }
        for (URI root : loader.roots) {
            URI rel = root.relativize(uri);
            if (!rel.equals(uri)) {
                return rel.getSchemeSpecificPart();
            }
        }
        return null; // tycho+maven hack
    }

    protected String jarPath(String jarPath, String filePath) {
        return filePath.substring(5 + jarPath.length() + 2);
    }

    protected String filePath(String rootPath, String filePath) {
        return filePath.substring(rootPath.length() + 1);
    }

    public void merge(OSGiWiring wiring) {
        for (Map.Entry<String, Set<OSGiLoader>> entry : wiring.contents.entrySet()) {
            wire(entry.getKey(), entry.getValue());
        }
    }

    public void substract(OSGiWiring wiring) {
        for (Map.Entry<String, Set<OSGiLoader>> entry : wiring.contents.entrySet()) {
            wire(entry.getKey(), entry.getValue());
        }
    }

    protected void load() {
        Enumeration<URL> entries = loader.listLocalFiles();
        while (entries.hasMoreElements()) {
            URL entry = entries.nextElement();
            String path = bundlePath(entry);
            if (path == null) {
                continue;
            }
            if (path.equals("META-INF/MANIFEST.MF")) {
                continue;
            }
            if (path.startsWith("META-INF")) {
                wire(path, loader);
                continue;
            }
            wire(shrinkPath(path), loader);
        }
    }

    public Set<OSGiLoader> mayContains(String path) {
        Set<OSGiLoader> loaders = contents.get(shrinkPath(path));
        if (loaders == null) {
            return Collections.emptySet();
        }
        return loaders;
    }

    protected String shrinkPath(String path) {
        if (path.startsWith("/")) {
            path = path.substring(1,path.length());
        }
        String components[] = path.split("/");
        return shrinkPath(3, components.length - 2, components);
    }

    protected String shrinkPath(int maxDepth, int index, String[] components) {
        if (maxDepth <= 0) {
            return "";
        }
        if (index <= 0) {
            return "/" + components[0];
        }
        return shrinkPath(maxDepth - 1, index - 1, components) + "/"
                + components[index];
    }

    public void clear() {
        contents.clear();
    }

}