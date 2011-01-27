/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.views;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.ElementType;
import org.nuxeo.theme.formats.FormatType;
import org.nuxeo.theme.models.ModelType;
import org.nuxeo.theme.templates.TemplateEngineType;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;

@XObject("view")
public final class ViewType implements Type {

    private static final Log log = LogFactory.getLog(ViewType.class);

    @XNode("@name")
    public String viewName = "*";

    @XNode("engine")
    public String engineName = "default";

    @XNode("@template-engine")
    private String templateEngine;

    @XNode("@merge")
    private boolean merge = false;

    @XNode("mode")
    public String mode = "*";

    @XNode("icon")
    public String icon;

    @XNode("element-type")
    public String elementTypeName = "*";

    @XNode("format-type")
    public String formatTypeName = "*";

    @XNode("model-type")
    public String modelTypeName = "*";

    @XNode("class")
    public String className;

    @XNode("template")
    public String template;

    @XNodeList(value = "resource", type = ArrayList.class, componentType = String.class)
    public List<String> resources = new ArrayList<String>();

    private View view;

    public ViewType() {
    }

    public ViewType(final String viewName, final String className,
            final String engineName, final String templateEngine,
            final String mode, final String elementTypeName,
            final String modelTypeName, final String formatTypeName,
            final String template, final List<String> resources) {
        this.viewName = viewName;
        this.elementTypeName = elementTypeName;
        this.modelTypeName = modelTypeName;
        this.formatTypeName = formatTypeName;
        this.engineName = engineName;
        this.templateEngine = templateEngine;
        this.mode = mode;
        this.className = className;
        this.template = template;
        this.resources = resources;
    }

    public String getTypeName() {
        return computeName(formatTypeName, elementTypeName, viewName,
                modelTypeName, engineName, mode, templateEngine);
    }

    public static String computeName(final String formatTypeName,
            final String elementTypeName, final String viewName,
            final String modelTypeName, final String engineName,
            final String mode, final String templateEngineName) {

        return String.format("%s/%s/%s/%s/%s/%s/%s", formatTypeName,
                elementTypeName, viewName, modelTypeName, engineName, mode,
                templateEngineName);
    }

    public TypeFamily getTypeFamily() {
        return TypeFamily.VIEW;
    }

    public String getViewName() {
        return viewName;
    }

    public View getView() {
        if (view != null) {
            return view;
        }
        if (className == null) {
            className = ((TemplateEngineType) Manager.getTypeRegistry().lookup(
                    TypeFamily.TEMPLATE_ENGINE, templateEngine)).getTemplateView();
        }
        try {
            view = (View) Class.forName(className).newInstance();
            view.setViewType(this);
        } catch (Exception e) {
            log.error("Could not create view for: " + className);
            return null;
        }
        return view;
    }

    public ElementType getElementType() {
        return (ElementType) Manager.getTypeRegistry().lookup(
                TypeFamily.ELEMENT, elementTypeName);
    }

    public ModelType getModelType() {
        return (ModelType) Manager.getTypeRegistry().lookup(TypeFamily.MODEL,
                modelTypeName);
    }

    public FormatType getFormatType() {
        return (FormatType) Manager.getTypeRegistry().lookup(TypeFamily.FORMAT,
                formatTypeName);
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(final String template) {
        this.template = template;
    }

    public List<String> getResources() {
        return resources;
    }

    public void setResources(final List<String> resources) {
        this.resources = resources;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getTemplateEngine() {
        return templateEngine;
    }

    public void setTemplateEngine(String templateEngine) {
        this.templateEngine = templateEngine;
    }

    public boolean isMerge() {
        return merge;
    }

    public void setMerge(boolean merge) {
        this.merge = merge;
    }

    public void addResource(String resource) {
        if (!resources.contains(resource)) {
            resources.add(resource);
        }
    }
}
