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

    // the parent fragment if any
    protected WebApplicationDescriptor next;
    protected boolean isRemoved = false;

    @XNode("@id")
    protected String id;

    @XNode("@fragment")
    protected String fragment;

    @XNodeList(value="roots/root", type=ArrayList.class, componentType=RootDescriptor.class, nullByDefault=true)
    protected List<RootDescriptor> roots;

    @XNode("errorPage")
    protected String errorPage;

    @XNode("indexPage")
    protected String indexPage;

    @XNode("defaultPage")
    protected String defaultPage;

    @XNode("documentResolver")
    protected Class<?> documentResolverClass;

    @XNodeList(value="mappings/mapping", type=ArrayList.class, componentType=MappingDescriptor.class, nullByDefault=true)
    protected List<MappingDescriptor> mappings;

    @XNodeList(value="bindings/binding", type=ArrayList.class, componentType=ObjectBindingDescriptor.class, nullByDefault=true)
    protected List<ObjectBindingDescriptor> bindings;

    @XNodeList(value="transformers/transformer", type=ArrayList.class, componentType=String.class, nullByDefault=true)
    protected List<String> transformers;

    @XNodeList(value="templates/template", type=ArrayList.class, componentType=String.class, nullByDefault=true)
    protected List<String> templates;

    /**
     * @param next the parent to set.
     */
    public void setNext(WebApplicationDescriptor next) {
        this.next = next;
    }

    /**
     * @return the parent.
     */
    public WebApplicationDescriptor next() {
        return next;
    }

    /**
     * @return the isRemoved.
     */
    public boolean isRemoved() {
        return isRemoved;
    }

    /**
     * @param isRemoved the isRemoved to set.
     */
    public void setRemoved(boolean isRemoved) {
        this.isRemoved = isRemoved;
    }

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
        return indexPage;
    }

    public String getIndexPage(String defaultValue) {
        return indexPage == null ? defaultValue : indexPage;
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
        return defaultPage;
    }

    public String getDefaultPage(String defaultValue) {
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
        return errorPage;
    }

    public String getErrorPage(String defaultValue) {
        return errorPage == null ? defaultValue : errorPage;
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
    public List<RootDescriptor> getRoots() {
        return roots;
    }

    public void setRoots(ArrayList<RootDescriptor> descriptors) {
        roots = descriptors;
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
     * @return the templates.
     */
    public List<String> getTemplates() {
        return templates;
    }

    /**
     * @param templates the templates to set.
     */
    public void setTemplates(List<String> templates) {
        this.templates = templates;
    }

    /**
     * @return the transformers.
     */
    public List<String> getTransformers() {
        return transformers;
    }

    /**
     * @param transformers the transformers to set.
     */
    public void setTransformers(List<String> transformers) {
        this.transformers = transformers;
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
        if (documentResolverClass != null) {
            try {
                return (DocumentResolver)documentResolverClass.newInstance();
            } catch (Exception e) {
                throw new WebDeployException("Failed to instantiate Resquest handler class: "+documentResolverClass, e);
            }
        }
        return null;
    }

    public void copyTo(WebApplicationDescriptor desc) {
        desc.id = id;
        if (defaultPage != null) {
            desc.defaultPage = defaultPage;
        }
        if (indexPage != null) {
            desc.indexPage = indexPage;
        }
        if (errorPage != null) {
            desc.errorPage = errorPage;
        }
        if (documentResolverClass != null) {
            desc.documentResolverClass = documentResolverClass;
        }
        if (roots != null && !roots.isEmpty()) {
            if (desc.roots == null) {
                desc.roots = new ArrayList<RootDescriptor>();
            }
            desc.roots.addAll(roots);
        }
        if (bindings != null && !bindings.isEmpty()) {
            if (desc.bindings == null) {
                desc.bindings = new ArrayList<ObjectBindingDescriptor>();
            }
            desc.bindings.addAll(bindings);
        }
        if (mappings != null && !mappings.isEmpty()) {
            if (desc.mappings == null) {
                desc.mappings = new ArrayList<MappingDescriptor>();
            }
            desc.mappings.addAll(mappings);
        }
        if (transformers != null && !transformers.isEmpty()) {
            if (desc.transformers == null) {
                desc.transformers = new ArrayList<String>();
            }
            desc.transformers.addAll(transformers);
        }
        if (templates != null && !templates.isEmpty()) {
            if (desc.templates == null) {
                desc.templates = new ArrayList<String>();
            }
            desc.templates.addAll(templates);
        }
    }


    @Override
    public String toString() {
        return id+"@"+fragment;
    }
}
