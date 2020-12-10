/*
 * (C) Copyright 2006-2020 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *     Bogdan Stefanescu
 *     Anahide Tchertchian
 */
package org.nuxeo.common.xmap;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XContext;
import org.nuxeo.common.xmap.annotation.XMemberAnnotation;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.annotation.XParent;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.common.xmap.registry.Registry;
import org.nuxeo.common.xmap.registry.SingleRegistry;
import org.nuxeo.common.xmap.registry.XEnable;
import org.nuxeo.common.xmap.registry.XMerge;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;
import org.nuxeo.common.xmap.registry.XRemove;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * XMap maps an XML file to a java object.
 * <p>
 * The mapping is described by annotations on java objects.
 * <p>
 * The following annotations are supported:
 * <ul>
 * <li>{@link XObject} Mark the object as being mappable to an XML node
 * <li>{@link XNode} Map an XML node to a field of a mappable object
 * <li>{@link XNodes} Map multiple XML nodes to a field of a mappable object
 * <li>{@link XNodeList} Map an list of XML nodes to a field of a mappable object
 * <li>{@link XNodeMap} Map an map of XML nodes to a field of a mappable object
 * <li>{@link XContent} Map an XML node content to a field of a mappable object
 * <li>{@link XParent} Map a field of the current mappable object to the parent object if any exists The parent object
 * is the mappable object containing the current object as a field
 * </ul>
 * <p>
 * The following registry-related annotations are also taken into account:
 * <ul>
 * <li>{@link XRegistry} Mark an object to be pushed to a {@link Registry}
 * <li>{@link XRegistryId} Map an XML node to an identifier to be used by a registry
 * <li>{@link XMerge} Control the object merge behavior in the target registry
 * <li>{@link XEnable} Control the object enablement behavior in the target registry
 * <li>{@link XRemove } Control the object removal behavior in the target registry
 * </ul>
 * <p>
 * The mapping is done in 2 steps:
 * <ul>
 * <li>The XML file is loaded as a DOM document
 * <li>The DOM document is parsed and the nodes mapping is resolved
 * </ul>
 * Registries hold additional logic to process DOM elements, applying merge/enablement/removal on-the-fly when
 * retrieving contributed objects.
 */
public class XMap {

    private static DocumentBuilderFactory initFactory() {
        Thread t = Thread.currentThread();
        ClassLoader cl = t.getContextClassLoader();
        t.setContextClassLoader(XMap.class.getClassLoader());
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            return factory;
        } finally {
            t.setContextClassLoader(cl);
        }
    }

    public static DocumentBuilderFactory getFactory() {
        return factory;
    }

    private static DocumentBuilderFactory factory = initFactory();

    // top level objects
    private final Map<String, XAnnotatedObject> roots;

    // the scanned objects
    private final Map<Class<?>, XAnnotatedObject> objects;

    private final Map<Class<?>, XValueFactory> factories;

    /**
     * Creates a new XMap object.
     */
    public XMap() {
        objects = new Hashtable<>();
        roots = new Hashtable<>();
        factories = new Hashtable<>(XValueFactory.defaultFactories);
    }

    /**
     * Gets the value factory used for objects of the given class.
     * <p>
     * Value factories are used to decode values from XML strings.
     *
     * @param type the object type
     * @return the value factory if any, null otherwise
     */
    public XValueFactory getValueFactory(Class<?> type) {
        return factories.get(type);
    }

    /**
     * Sets a custom value factory for the given class.
     * <p>
     * Value factories are used to decode values from XML strings.
     *
     * @param type the object type
     * @param factory the value factory to use for the given type
     */
    public void setValueFactory(Class<?> type, XValueFactory factory) {
        factories.put(type, factory);
    }

    /**
     * Gets a list of scanned objects.
     * <p>
     * Scanned objects are annotated objects that were registered by this XMap instance.
     */
    public Collection<XAnnotatedObject> getScannedObjects() {
        return objects.values();
    }

    /**
     * Gets the root objects.
     * <p>
     * Root objects are scanned objects that can be mapped to XML elements that are not part from other objects.
     *
     * @return the root objects
     */
    public Collection<XAnnotatedObject> getRootObjects() {
        return roots.values();
    }

    /**
     * Registers a mappable object class.
     * <p>
     * The class will be scanned for XMap annotations and a mapping description is created.
     *
     * @param klass the object class
     * @return the mapping description
     */
    public XAnnotatedObject register(Class<?> klass) {
        XAnnotatedObject xao = objects.get(klass);
        if (xao == null) { // avoid scanning twice
            XObject xob = klass.getAnnotation(XObject.class);
            if (xob != null) {
                xao = new XAnnotatedObject(this, klass, xob);
                objects.put(xao.klass, xao);
                scanObjectRegistryAnnotations(xao, xao.klass);
                scanClass(xao, xao.klass);
                String key = xob.value();
                if (key.length() > 0) {
                    roots.put(xao.path.path, xao);
                }
            }
        }
        return xao;
    }

    /**
     * Resolves the registry for given object.
     * <p>
     * The registry is resolved thanks to the {@link XRegistry} and {@link XRegistryId} annotations resolved for this
     * object.
     * <p>
     * Returns null if not annotation is found.
     *
     * @since 11.5
     */
    public Registry getRegistry(XAnnotatedObject xObject) {
        if (xObject == null || !xObject.hasRegistry()) {
            return null;
        }
        if (xObject.getRegistryId() != null) {
            return new MapRegistry();
        } else {
            return new SingleRegistry();
        }
    }

    private void scanClass(XAnnotatedObject xob, Class<?> klass) {
        Field[] fields = klass.getDeclaredFields();
        for (Field field : fields) {
            Annotation anno = checkMemberAnnotation(field);
            if (anno != null) {
                XAccessor setter = new XFieldAccessor(field);
                XAnnotatedMember member = createMember(anno, setter);
                xob.addMember(member);
                if (anno instanceof XNode) {
                    scanMemberRegistryAnnotations(xob, (XNode) anno, field);
                }
                if (member instanceof XAnnotatedList) {
                    scanMergeAnnotations(xob, field, (XAnnotatedList) member);
                }
            }
        }

        Method[] methods = klass.getDeclaredMethods();
        for (Method method : methods) {
            // we accept only methods with one parameter
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length != 1) {
                continue;
            }
            Annotation anno = checkMemberAnnotation(method);
            if (anno != null) {
                XAccessor setter = new XMethodAccessor(method, klass);
                XAnnotatedMember xm = createMember(anno, setter);
                xob.addMember(xm);
                if (anno instanceof XNode) {
                    scanMemberRegistryAnnotations(xob, (XNode) anno, method);
                }
            }
        }

        // scan superClass annotations
        if (klass.getSuperclass() != null) {
            scanClass(xob, klass.getSuperclass());
        }
    }

    private void scanObjectRegistryAnnotations(XAnnotatedObject xob, Class<?> klass) {
        XRegistry xreg = klass.getAnnotation(XRegistry.class);
        xob.setHasRegistry(xreg != null);
        XRegistryId xregistryId = klass.getAnnotation(XRegistryId.class);
        if (xregistryId != null) {
            xob.setRegistryId(
                    new XAnnotatedReference(this, String.class, xregistryId.value(), xregistryId.fallback(), null));
        }
        if (xreg != null) {
            if (xreg.merge()) {
                xob.setMerge(new XAnnotatedReference(this, XMerge.MERGE, null, true));
            }
            if (xreg.enable()) {
                xob.setEnable(new XAnnotatedReference(this, XEnable.ENABLE, null, true));
            }
            if (xreg.remove()) {
                xob.setRemove(new XAnnotatedReference(this, XRemove.REMOVE, null, false));
            }
        }
    }

    private void scanMemberRegistryAnnotations(XAnnotatedObject xob, XNode annotation, AnnotatedElement ae) {
        if (xob.getRegistryId() == null && ae.isAnnotationPresent(XRegistryId.class)) {
            xob.setRegistryId(new XAnnotatedReference(this, String.class, annotation.value(), annotation.fallback(),
                    annotation.defaultAssignment()));
        }
        if (xob.getMerge() == null && ae.isAnnotationPresent(XMerge.class)) {
            XMerge merge = ae.getAnnotation(XMerge.class);
            xob.setMerge(new XAnnotatedReference(this, annotation.value(), annotation.fallback(),
                    merge.defaultAssignment()));
        }
        if (xob.getEnable() == null && ae.isAnnotationPresent(XEnable.class)) {
            xob.setEnable(new XAnnotatedReference(this, annotation.value(), annotation.fallback(),
                    ae.getAnnotation(XEnable.class).defaultAssignment()));
        }
        if (xob.getRemove() == null && ae.isAnnotationPresent(XRemove.class)) {
            xob.setRemove(new XAnnotatedReference(this, annotation.value(), annotation.fallback(),
                    ae.getAnnotation(XRemove.class).defaultAssignment()));
        }
    }

    private void scanMergeAnnotations(XAnnotatedObject xob, AnnotatedElement ae, XAnnotatedList member) {
        if (ae.isAnnotationPresent(XMerge.class)) {
            XMerge merge = ae.getAnnotation(XMerge.class);
            member.setMerge(new XAnnotatedReference(this, merge.value(), merge.fallback(), merge.defaultAssignment()));
        }
        if (ae.isAnnotationPresent(XRemove.class)) {
            XRemove remove = ae.getAnnotation(XRemove.class);
            member.setRemove(
                    new XAnnotatedReference(this, remove.value(), remove.fallback(), remove.defaultAssignment()));
        }
    }

    /**
     * Processes the XML file at the given URL using a default context.
     *
     * @param url the XML file url
     * @return the first registered top level object that is found in the file, or null if no objects are found.
     */
    public Object load(URL url) throws IOException {
        return load(new Context(), url.openStream());
    }

    /**
     * Processes the XML file at the given URL and using the given contexts.
     *
     * @param ctx the context to use
     * @param url the XML file url
     * @return the first registered top level object that is found in the file.
     */
    public Object load(Context ctx, URL url) throws IOException {
        return load(ctx, url.openStream());
    }

    /**
     * Processes the XML content from the given input stream using a default context.
     *
     * @param in the XML input source
     * @return the first registered top level object that is found in the file.
     */
    public Object load(InputStream in) throws IOException {
        return load(new Context(), in);
    }

    /**
     * Processes the XML content from the given input stream using the given context.
     *
     * @param ctx the context to use
     * @param in the input stream
     * @return the first registered top level object that is found in the file.
     */
    public Object load(Context ctx, InputStream in) throws IOException {
        try {
            DocumentBuilderFactory factory = getFactory();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(in);
            return load(ctx, document.getDocumentElement());
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // do nothing
                }
            }
        }
    }

    /**
     * Processes the XML file at the given URL using a default context.
     * <p>
     * Returns a list with all registered top level objects that are found in the file.
     * <p>
     * If not objects are found, an empty list is returned.
     *
     * @param url the XML file url
     * @return a list with all registered top level objects that are found in the file
     */
    public Object[] loadAll(URL url) throws IOException {
        return loadAll(new Context(), url.openStream());
    }

    /**
     * Processes the XML file at the given URL using the given context
     * <p>
     * Return a list with all registered top level objects that are found in the file.
     * <p>
     * If not objects are found an empty list is retoruned.
     *
     * @param ctx the context to use
     * @param url the XML file url
     * @return a list with all registered top level objects that are found in the file
     */
    public Object[] loadAll(Context ctx, URL url) throws IOException {
        return loadAll(ctx, url.openStream());
    }

    /**
     * Processes the XML from the given input stream using the given context.
     * <p>
     * Returns a list with all registered top level objects that are found in the file.
     * <p>
     * If not objects are found, an empty list is returned.
     *
     * @param in the XML input stream
     * @return a list with all registered top level objects that are found in the file
     */
    public Object[] loadAll(InputStream in) throws IOException {
        return loadAll(new Context(), in);
    }

    /**
     * Processes the XML from the given input stream using the given context.
     * <p>
     * Returns a list with all registered top level objects that are found in the file.
     * <p>
     * If not objects are found, an empty list is returned.
     *
     * @param ctx the context to use
     * @param in the XML input stream
     * @return a list with all registered top level objects that are found in the file
     */
    public Object[] loadAll(Context ctx, InputStream in) throws IOException {
        try {
            DocumentBuilderFactory factory = getFactory();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(in);
            return loadAll(ctx, document.getDocumentElement());
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // do nothing
                }
            }
        }
    }

    /**
     * Processes the given DOM element and return the first mappable object found in the element.
     * <p>
     * A default context is used.
     *
     * @param root the element to process
     * @return the first object found in this element or null if none
     */
    public Object load(Element root) {
        return load(new Context(), root);
    }

    /**
     * Processes the given DOM element and return the first mappable object found in the element.
     * <p>
     * The given context is used.
     *
     * @param ctx the context to use
     * @param root the element to process
     * @return the first object found in this element or null if none
     */
    public Object load(Context ctx, Element root) {
        // check if the current element is bound to an annotated object
        String name = root.getNodeName();
        XAnnotatedObject xob = roots.get(name);
        if (xob != null) {
            return xob.newInstance(ctx, root);
        } else {
            Node p = root.getFirstChild();
            while (p != null) {
                if (p.getNodeType() == Node.ELEMENT_NODE) {
                    // Recurse in the first child Element
                    return load(ctx, (Element) p);
                }
                p = p.getNextSibling();
            }
            // We didn't find any Element
            return null;
        }
    }

    /**
     * Processes the given DOM element and return a list with all top-level mappable objects found in the element.
     * <p>
     * The given context is used.
     *
     * @param ctx the context to use
     * @param root the element to process
     * @return the list of all top level objects found
     */
    public Object[] loadAll(Context ctx, Element root) {
        List<Object> result = new ArrayList<>();
        loadAll(ctx, root, result);
        return result.toArray();
    }

    /**
     * Processes the given DOM element and return a list with all top-level mappable objects found in the element.
     * <p>
     * The default context is used.
     *
     * @param root the element to process
     * @return the list of all top level objects found
     */
    public Object[] loadAll(Element root) {
        return loadAll(new Context(), root);
    }

    /**
     * Same as {@link XMap#loadAll(Element)} but put collected objects in the given collection.
     *
     * @param root the element to process
     * @param result the collection where to collect objects
     */
    public void loadAll(Element root, Collection<Object> result) {
        loadAll(new Context(), root, result);
    }

    /**
     * Same as {@link XMap#loadAll(Context, Element)} but put collected objects in the given collection.
     *
     * @param ctx the context to use
     * @param root the element to process
     * @param result the collection where to collect objects
     */
    public void loadAll(Context ctx, Element root, Collection<Object> result) {
        // check if the current element is bound to an annotated object
        String name = root.getNodeName();
        XAnnotatedObject xob = roots.get(name);
        if (xob != null) {
            Object ob = xob.newInstance(ctx, root);
            result.add(ob);
        } else {
            Node p = root.getFirstChild();
            while (p != null) {
                if (p.getNodeType() == Node.ELEMENT_NODE) {
                    loadAll(ctx, (Element) p, result);
                }
                p = p.getNextSibling();
            }
        }
    }

    /**
     * Returns the resolved annotation object for given class.
     *
     * @since 11.5
     */
    public XAnnotatedObject getObject(Class<?> klass) {
        return objects.get(klass);
    }

    /**
     * Registers contributions with given tag on given registry.
     *
     * @since 11.5
     */
    public void register(Registry registry, Context ctx, Element root, String tag) {
        if (registry == null || registry.isNull()) {
            return;
        }
        String name = root.getNodeName();
        XAnnotatedObject xob = roots.get(name);
        if (xob != null) {
            registry.register(ctx, xob, root, tag);
        } else {
            Node p = root.getFirstChild();
            while (p != null) {
                if (p.getNodeType() == Node.ELEMENT_NODE) {
                    register(registry, ctx, (Element) p, tag);
                }
                p = p.getNextSibling();
            }
        }
    }

    /**
     * Unregisters contributions with given tag on given registry.
     *
     * @since 11.5
     */
    public void unregister(Registry registry, String tag) {
        if (registry == null || registry.isNull()) {
            return;
        }
        registry.unregister(tag);
    }

    protected static Annotation checkMemberAnnotation(AnnotatedElement ae) {
        Annotation[] annos = ae.getAnnotations();
        for (Annotation anno : annos) {
            if (anno.annotationType().isAnnotationPresent(XMemberAnnotation.class)) {
                return anno;
            }
        }
        return null;
    }

    private XAnnotatedMember createMember(Annotation annotation, XAccessor setter) {
        XAnnotatedMember member = null;
        int type = annotation.annotationType().getAnnotation(XMemberAnnotation.class).value();
        if (type == XMemberAnnotation.NODE) {
            member = new XAnnotatedMember(this, setter, (XNode) annotation);
        } else if (type == XMemberAnnotation.NODE_LIST) {
            member = new XAnnotatedList(this, setter, (XNodeList) annotation);
        } else if (type == XMemberAnnotation.NODE_MAP) {
            member = new XAnnotatedMap(this, setter, (XNodeMap) annotation);
        } else if (type == XMemberAnnotation.PARENT) {
            member = new XAnnotatedParent(this, setter);
        } else if (type == XMemberAnnotation.CONTENT) {
            member = new XAnnotatedContent(this, setter, (XContent) annotation);
        } else if (type == XMemberAnnotation.CONTEXT) {
            member = new XAnnotatedContext(this, setter, (XContext) annotation);
        }
        return member;
    }

    // methods to serialize the map
    public String toXML(Object object) throws IOException {
        DocumentBuilderFactory dbfac = getFactory();
        DocumentBuilder docBuilder;
        try {
            docBuilder = dbfac.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        }
        Document doc = docBuilder.newDocument();
        // create root element
        Element root = doc.createElement("root");
        doc.appendChild(root);

        // load xml reprezentation in root
        toXML(object, root);
        return DOMSerializer.toString(root);
    }

    public void toXML(Object object, OutputStream os) throws IOException {
        String xml = toXML(object);
        os.write(xml.getBytes());
    }

    public void toXML(Object object, File file) throws IOException {
        String xml = toXML(object);
        FileUtils.writeStringToFile(file, xml, UTF_8);
    }

    public void toXML(Object object, Element root) {
        XAnnotatedObject xao = objects.get(object.getClass());
        if (xao == null) {
            throw new IllegalArgumentException(object.getClass().getCanonicalName() + " is NOT registred in xmap");
        }
        XMLBuilder.saveToXML(object, root, xao);
    }

}
