/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.common.xmap;

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

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XContext;
import org.nuxeo.common.xmap.annotation.XMemberAnnotation;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.annotation.XParent;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


/**
 * XMap maps an XML file to a java object.
 * <p>
 * The mapping is described by annotations on java objects.
 * <p>
 * The following annotations are supported:
 * <ul>
 * <li> {@link XObject}
 * Mark the object as being mappable to an XML node
 * <li> {@link XNode}
 * Map an XML node to a field of a mappable object
 * <li> {@link XNodeList}
 * Map an list of XML nodes to a field of a mappable object
 * <li> {@link XNodeMap}
 * Map an map of XML nodes to a field of a mappable object
 * <li> {@link XContent}
 * Map an XML node content to a field of a mappable object
 * <li> {@link XParent}
 * Map a field of the current mappable object to the parent object if any exists
 * The parent object is the mappable object containing the current object as a field
 * </ul>
 *
 * The mapping is done in 2 steps:
 * <ul>
 * <li> The XML file is loaded as a DOM document
 * <li> The DOM document is parsed and the nodes mapping is resolved
 * </ul>
 *
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@SuppressWarnings({"SuppressionAnnotation"})
public class XMap {

    private static final DocumentBuilderFactory initFactory() {
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
        objects = new Hashtable<Class<?>, XAnnotatedObject>();
        roots = new Hashtable<String, XAnnotatedObject>();
        factories = new Hashtable<Class<?>, XValueFactory>(XValueFactory.defaultFactories);
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
     * Scanned objects are annotated objects that were registered
     * by this XMap instance.
     */
    public Collection<XAnnotatedObject> getScannedObjects() {
        return objects.values();
    }

    /**
     * Gets the root objects.
     * <p>
     * Root objects are scanned objects that can be mapped to XML elements
     * that are not part from other objects.
     *
     * @return the root objects
     */
    public Collection<XAnnotatedObject> getRootObjects() {
        return roots.values();
    }

    /**
     * Registers a mappable object class.
     * <p>
     * The class will be scanned for XMap annotations
     * and a mapping description is created.
     *
     * @param klass the object class
     * @return the mapping description
     */
    public XAnnotatedObject register(Class<?> klass) {
        XAnnotatedObject xao = objects.get(klass);
        if (xao == null) { // avoid scanning twice
            XObject xob = checkObjectAnnotation(klass);
            if (xob != null) {
                xao = new XAnnotatedObject(this, klass, xob);
                objects.put(xao.klass, xao);
                scan(xao);
                String key = xob.value();
                if (key.length() > 0) {
                    roots.put(xao.path.path, xao);
                }
            }
        }
        return xao;
    }

    private void scan(XAnnotatedObject xob) {
        scanClass(xob, xob.klass);
    }

    private void scanClass(XAnnotatedObject xob, Class<?> aClass) {
        Field[] fields = aClass.getDeclaredFields();
        for (Field field : fields) {
            Annotation anno =  checkMemberAnnotation(field);
            if (anno != null) {
                XAnnotatedMember member = createFieldMember(field, anno);
                xob.addMember(member);
            }
        }

        Method[] methods = aClass.getDeclaredMethods();
        for (Method method : methods) {
            // we accept only methods with one parameter
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length != 1) {
                continue;
            }
            Annotation anno =  checkMemberAnnotation(method);
            if (anno != null) {
                XAnnotatedMember member = createMethodMember(method, anno, aClass);
                xob.addMember(member);
            }
        }

        // scan superClass annotations
        if (aClass.getSuperclass() != null) {
            scanClass(xob, aClass.getSuperclass());
        }
    }

    /**
     * Processes the XML file at the given URL using a default context.
     *
     * @param url the XML file url
     * @return the first registered top level object that is found in the file,
     *    or null if no objects are found.
     */
    public Object load(URL url) throws Exception {
        return load(new Context(), url.openStream());
    }

    /**
     * Processes the XML file at the given URL and using the given contexts.
     *
     * @param ctx the context to use
     * @param url the XML file url
     * @return the first registered top level object that is found in the file.
     */
    public Object load(Context ctx, URL url) throws Exception {
        return load(ctx, url.openStream());
    }

    /**
     * Processes the XML content from the given input stream using a default context.
     *
     * @param in the XML input source
     * @return the first registered top level object that is found in the file.
     */
    public Object load(InputStream in) throws Exception {
        return load(new Context(), in);
    }

    /**
     * Processes the XML content from the given input stream using the given context.
     *
     * @param ctx the context to use
     * @param in the input stream
     * @return the first registered top level object that is found in the file.
     */
    public Object load(Context ctx, InputStream in) throws Exception {
        try {
            DocumentBuilderFactory factory = getFactory();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(in);
            return load(ctx, document.getDocumentElement());
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
    public Object[] loadAll(URL url) throws Exception {
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
    public Object[] loadAll(Context ctx, URL url) throws Exception {
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
    public Object[] loadAll(InputStream in) throws Exception {
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
    public Object[] loadAll(Context ctx, InputStream in) throws Exception {
        try {
            DocumentBuilderFactory factory = getFactory();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(in);
            return loadAll(ctx, document.getDocumentElement());
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
     * Processes the given DOM element and return the first mappable object
     * found in the element.
     * <p>
     * A default context is used.
     *
     * @param root the element to process
     * @return the first object found in this element or null if none
     */
    public Object load(Element root) throws Exception {
        return load(new Context(), root);
    }

    /**
     * Processes the given DOM element and return the first mappable object
     * found in the element.
     * <p>
     * The given context is used.
     *
     * @param ctx the context to use
     * @param root the element to process
     * @return the first object found in this element or null if none
     */
    public Object load(Context ctx, Element root) throws Exception {
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
                    return load((Element) p);
                }
                p = p.getNextSibling();
            }
            // We didn't find any Element
            return null;
        }
    }

    /**
     * Processes the given DOM element and return a list with all top-level
     * mappable objects found in the element.
     * <p>
     * The given context is used.
     *
     * @param ctx the context to use
     * @param root the element to process
     * @return the list of all top level objects found
     */
    public Object[] loadAll(Context ctx, Element root) throws Exception {
        List<Object> result = new ArrayList<Object>();
        loadAll(ctx, root, result);
        return result.toArray();
    }

    /**
     * Processes the given DOM element and return a list with all top-level
     * mappable objects found in the element.
     * <p>
     * The default context is used.
     *
     * @param root the element to process
     * @return the list of all top level objects found
     */
    public Object[] loadAll(Element root) throws Exception {
        return loadAll(new Context(), root);
    }

    /**
     * Same as {@link XMap#loadAll(Element)} but put collected objects in the
     * given collection.
     *
     * @param root the element to process
     * @param result the collection where to collect objects
     */
    public void loadAll(Element root, Collection<Object> result) throws Exception {
        loadAll(new Context(), root, result);
    }

    /**
     * Same as {@link XMap#loadAll(Context, Element)} but put collected objects in the
     * given collection.
     *
     * @param ctx the context to use
     * @param root the element to process
     * @param result the collection where to collect objects
     */
    public void loadAll(Context ctx, Element root, Collection<Object> result) throws Exception {
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

    protected static Annotation checkMemberAnnotation(AnnotatedElement ae) {
        Annotation[] annos = ae.getAnnotations();
        for (Annotation anno : annos) {
            if (anno.annotationType()
                    .isAnnotationPresent(XMemberAnnotation.class)) {
                return anno;
            }
        }
        return null;
    }

    protected static XObject checkObjectAnnotation(AnnotatedElement ae) {
        return ae.getAnnotation(XObject.class);
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

    public final XAnnotatedMember createFieldMember(Field field, Annotation annotation) {
        XAccessor setter = new XFieldAccessor(field);
        return createMember(annotation, setter);
    }

    public final XAnnotatedMember createMethodMember(Method method, Annotation annotation, Class<?> klass) {
        XAccessor setter = new XMethodAccessor(method, klass);
        return createMember(annotation, setter);
    }


    // methods to serialize the map
    public String toXML(Object object) throws ParserConfigurationException, IOException{
        DocumentBuilderFactory dbfac = getFactory();
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        // create root element
        Element root = doc.createElement("root");
        doc.appendChild(root);

        // load xml reprezentation in root
        toXML(object, root);
        return DOMSerializer.toString(root);
    }

    public void toXML(Object object, OutputStream os ) throws Exception{
        String xml = toXML(object);
        os.write(xml.getBytes());
    }

    public void toXML(Object object, File file) throws Exception{
        String xml = toXML(object);
        FileUtils.writeFile(file, xml);
    }

    public void toXML(Object object, Element root){
        XAnnotatedObject xao = objects.get(object.getClass());
        if ( xao == null ){
            throw new IllegalArgumentException(object.getClass().getCanonicalName() + " is NOT registred in xmap");
        }
        XMLBuilder.saveToXML(object, root, xao);
    }

}
