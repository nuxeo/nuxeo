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

package org.nuxeo.ecm.webengine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.webengine.exceptions.WebDeployException;
import org.nuxeo.ecm.webengine.mapping.MappingDescriptor;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("webapp")
public class WebApplicationDescriptor {

    @XNode("@id")
    protected String id;

    protected String[] roots;

    @XNode("errorPage")
    protected String errorPage;

    @XNode("indexPage")
    protected String indexPage;

    @XNode("defaultPage")
    protected String defaultPage;

    @XNode("documentResolver")
    protected Class<?> documentResolverClass;

    @XNodeList(value="mappings/mapping", type=ArrayList.class, componentType=MappingDescriptor.class)
    protected List<MappingDescriptor> mappings;

    @XNodeList(value="bindings/binding", type=ArrayList.class, componentType=ObjectBindingDescriptor.class)
    protected List<ObjectBindingDescriptor> bindings;

    /**
     * @return the id.
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the indexPage.
     */
    public String getIndexPage() {
        return indexPage == null ? "index.ftl" : indexPage;
    }

    /**
     * @param indexPage the indexPage to set.
     */
    public void setIndexPage(String indexPage) {
        this.indexPage = indexPage;
    }

    /**
     * @return the defaultPage.
     */
    public String getDefaultPage() {
        return defaultPage == null ? "default.ftl" : defaultPage;
    }

    /**
     * @param defaultPage the defaultPage to set.
     */
    public void setDefaultPage(String defaultPage) {
        this.defaultPage = defaultPage;
    }

    /**
     * @return the errorPage.
     */
    public String getErrorPage() {
        return errorPage == null ? "error.ftl" : errorPage;
    }

    /**
     * @param errorPage the errorPage to set.
     */
    public void setErrorPage(String errorPage) {
        this.errorPage = errorPage;
    }

    /**
     * @return the roots.
     */
    public String[] getRoots() {
        return roots == null || roots.length == 0 ? new String[] {"default"} : roots;
    }

    /**
     * @param roots the roots to set.
     */
    public void setRoots(String[] roots) {
        this.roots = roots;
    }

    @XNodeList(value="roots/root", type=RootDescriptor[].class, componentType=RootDescriptor.class)
    public void setRoots(RootDescriptor[] descriptors) {
        if (descriptors == null || descriptors.length == 0) {
            roots = null;
        } else {
            Arrays.sort(descriptors);
            roots = new String[descriptors.length];
            for (int i=0; i<descriptors.length; i++) {
                roots[i] = descriptors[i].path;
            }
        }
    }

    /**
     * @return the mappings.
     */
    public List<MappingDescriptor> getMappings() {
        return mappings;
    }

    /**
     * @param mappings the mappings to set.
     */
    public void setMappings(List<MappingDescriptor> mappings) {
        this.mappings = mappings;
    }

    /**
     * @return the bindings.
     */
    public List<ObjectBindingDescriptor> getBindings() {
        return bindings;
    }

    /**
     * @param bindings the bindings to set.
     */
    public void setBindings(List<ObjectBindingDescriptor> bindings) {
        this.bindings = bindings;
    }

    /**
     * @return the documentResolverClass.
     */
    public Class<?> getDocumentResolverClass() {
        return documentResolverClass == null ? DefaultDocumentResolver.class : documentResolverClass;
    }

    /**
     * @param documentResolverClass the documentResolverClass to set.
     */
    public void setDocumentResolverClass(Class<?> documentResolverClass) {
        this.documentResolverClass = documentResolverClass;
    }

    public DocumentResolver getDocumentResolver() throws WebDeployException {
        if (documentResolverClass == null) {
            return new DefaultDocumentResolver();
        } else {
            try {
                return (DocumentResolver)documentResolverClass.newInstance();
            } catch (Exception e) {
                throw new WebDeployException("Failed to instantiate Resquest handler class: "+documentResolverClass, e);
            }
        }
    }

    public void merge(WebApplicationDescriptor parent) {
        //TODO
    }

}
