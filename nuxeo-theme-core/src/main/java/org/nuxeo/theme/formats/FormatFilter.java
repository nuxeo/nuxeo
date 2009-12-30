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

package org.nuxeo.theme.formats;

import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.elements.ElementType;
import org.nuxeo.theme.engines.EngineType;
import org.nuxeo.theme.fragments.Fragment;
import org.nuxeo.theme.models.Model;
import org.nuxeo.theme.models.ModelType;
import org.nuxeo.theme.rendering.AbstractFilter;
import org.nuxeo.theme.rendering.FilterTypeFamily;
import org.nuxeo.theme.rendering.RenderingInfo;
import org.nuxeo.theme.resources.ResourceManager;
import org.nuxeo.theme.templates.TemplateEngineType;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;
import org.nuxeo.theme.views.View;
import org.nuxeo.theme.views.ViewType;

public class FormatFilter extends AbstractFilter {

    private static final Log log = LogFactory.getLog(FormatFilter.class);

    private FormatType formatType;

    private final TypeRegistry typeRegistry;

    public FormatFilter() {
        typeRegistry = Manager.getTypeRegistry();
    }

    public FilterTypeFamily getFilterTypeFamily() {
        return FilterTypeFamily.FORMAT;
    }

    public FormatType getFormatType() {
        return formatType;
    }

    public void setFormatType(final FormatType formatType) {
        this.formatType = formatType;
    }

    @Override
    public RenderingInfo process(final RenderingInfo info, final boolean cache) {
        final EngineType engine = info.getEngine();
        final TemplateEngineType templateEngineType = info.getTemplateEngine();
        final String viewMode = info.getViewMode();
        final Element element = info.getElement();
        final ElementType elementType = element.getElementType();

        final Model model = info.getModel();
        final ModelType modelType = model == null ? null : model.getModelType();

        final Format format = info.getFormat();

        // look for a view by model type and by view name
        final View view = getView(format.getName(), engine, viewMode,
                elementType, modelType, formatType, templateEngineType);

        if (view == null) {
            // Sanity check
            if (model == null) {
                if (element instanceof Fragment) {
                    Fragment fragment = (Fragment) element;
                    ModelType expectedModelType = fragment.getFragmentType().getModelType();
                    if (expectedModelType != null) {
                        log.warn(String.format(
                                "Fragment %s should have returned a model of type: %s",
                                fragment.computeXPath(),
                                expectedModelType.getTypeName()));
                    }
                }
            } else {
                log.warn(String.format(
                        "No %s view named '%s' found for element %s (theme URL is: %s)",
                        formatType.getTypeName(), format.getName(),
                        element.computeXPath(), info.getThemeUrl().toString()));
            }
        } else {

            final String markup = view.render(info);
            info.setMarkup(markup);

            // Add resources used by the view (.css, .js, ...)
            final URL themeUrl = info.getThemeUrl();
            final ResourceManager resourceManager = Manager.getResourceManager();
            for (String resource : view.getViewType().getResources()) {
                resourceManager.addResource(resource, themeUrl);
            }
        }
        return info;
    }

    private View getView(final String viewName, final EngineType engine,
            final String viewMode, final ElementType elementType,
            final ModelType modelType, final FormatType formatType,
            final TemplateEngineType templateEngineType) {

        // allow to fall back on no specific view name
        final String effectiveViewName = (viewName == null || viewName.equals("")) ? "*"
                : viewName;
        final String effectiveViewMode = viewMode == null ? "*" : viewMode;
        final String engineName = engine == null ? "default" : engine.getName();
        final String templateEngineName = templateEngineType == null ? null
                : templateEngineType.getName();

        final String elementTypeName = elementType == null ? "*"
                : elementType.getTypeName();
        final String modelTypeName = modelType == null ? "*"
                : modelType.getTypeName();
        final String formatTypeName = formatType == null ? "*"
                : formatType.getTypeName();

        View view = getViewFor(formatTypeName, elementTypeName,
                effectiveViewName, modelTypeName, engineName,
                effectiveViewMode, templateEngineName);

        // fall back to unspecified element type
        if (view == null && !"*".equals(elementTypeName)) {
            view = getViewFor(formatTypeName, "*", effectiveViewName,
                    modelTypeName, engineName, effectiveViewMode,
                    templateEngineName);
        }

        // fall back to unspecified model type
        if (view == null && !"*".equals(modelTypeName)) {
            view = getViewFor(formatTypeName, elementTypeName,
                    effectiveViewName, "*", engineName, effectiveViewMode,
                    templateEngineName);
        }

        // fall back to unspecified element and model type
        if (view == null && !"*".equals(elementTypeName)
                && !"*".equals(modelTypeName)) {
            view = getViewFor(formatTypeName, "*", effectiveViewName, "*",
                    engineName, effectiveViewMode, templateEngineName);
        }

        // fall back to unspecified view name and unspecified model type
        if (view == null && !"*".equals(effectiveViewName)) {
            view = getViewFor(formatTypeName, elementTypeName, "*", "*",
                    engineName, effectiveViewMode, templateEngineName);
        }
        return view;
    }

    private View getViewFor(final String formatTypeName,
            final String elementTypeName, final String viewName,
            final String modelTypeName, final String engineName,
            final String viewMode, final String templateEngineName) {

        ViewType viewType = (ViewType) typeRegistry.lookup(
                TypeFamily.VIEW,
                ViewType.computeName(formatTypeName, elementTypeName, viewName,
                        modelTypeName, engineName, viewMode, templateEngineName));

        // Any view mode
        if (viewType == null && !"*".equals(viewMode)) {
            viewType = (ViewType) typeRegistry.lookup(TypeFamily.VIEW,
                    ViewType.computeName(formatTypeName, elementTypeName,
                            viewName, modelTypeName, engineName, "*",
                            templateEngineName));
        }

        // Any view mode, default engine
        if (viewType == null && !"default".equals(engineName)) {
            viewType = (ViewType) typeRegistry.lookup(TypeFamily.VIEW,
                    ViewType.computeName(formatTypeName, elementTypeName,
                            viewName, modelTypeName, "default", "*",
                            templateEngineName));
        }

        return viewType == null ? null : viewType.getView();
    }
}
