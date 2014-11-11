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

package org.nuxeo.theme.elements;

import java.io.StringWriter;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.engines.EngineType;
import org.nuxeo.theme.formats.Format;
import org.nuxeo.theme.formats.FormatFilter;
import org.nuxeo.theme.formats.FormatType;
import org.nuxeo.theme.fragments.Fragment;
import org.nuxeo.theme.models.InfoPool;
import org.nuxeo.theme.models.ModelException;
import org.nuxeo.theme.nodes.Node;
import org.nuxeo.theme.rendering.Filter;
import org.nuxeo.theme.rendering.FilterType;
import org.nuxeo.theme.rendering.FilterTypeFamily;
import org.nuxeo.theme.rendering.RendererType;
import org.nuxeo.theme.rendering.RenderingInfo;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;

public final class ElementRenderer {

    private static final Log log = LogFactory.getLog(ElementRenderer.class);

    private static final TypeRegistry typeRegistry = Manager.getTypeRegistry();

    private ElementRenderer() {
        // This class is not supposed to be instantiated.
    }

    public static RenderingInfo render(final RenderingInfo info) {
        return render(info, true);
    }

    public static RenderingInfo render(RenderingInfo info, final boolean cache) {
        InfoPool.register(info);

        final StringWriter rendered = new StringWriter();
        final URL themeUrl = info.getThemeUrl();
        if (themeUrl == null) {
            log.warn("Theme URL not set for the element: " + info.getElement());
        }

        final EngineType engine = info.getEngine();
        final Element element = info.getElement();

        String markup = "";
        if (element.isLeaf()) {
            if (!(element instanceof Fragment)) {
                log.error(String.format(
                        "Leaf nodes must be fragments, ignoring element: %s",
                        element.getElementType().getTypeName()));
                return info;
            }
            final Fragment fragment = (Fragment) element;
            try {
                info.setModel(fragment.getModel());
            } catch (ModelException e) {
                if (info.isDirty()) {
                    final String fragmentName = fragment.getFragmentType().getTypeName();
                    log.error("Rendering of fragment '" + fragmentName
                            + "' failed:");
                    e.printStackTrace();
                    return info;
                }
            }
            if (fragment.isDynamic()) {
                info.setDirty(true);
            }
        } else {
            for (Node child : element.getChildrenInContext(themeUrl)) {
                final RenderingInfo childInfo = new RenderingInfo(
                        (Element) child, themeUrl);
                final RenderingInfo renderedChild = render(childInfo);
                if (renderedChild == null) {
                    continue;
                }
                rendered.append(renderedChild.getMarkup());
            }
            markup = rendered.toString();
        }

        info.setMarkup(markup);

        final RendererType renderer = engine.getRenderers().get(
                element.getElementType().getTypeName());

        if (renderer == null) {
            return info;
        }

        final String templateEngineName = info.getTemplateEngine().getName();
        final String engineName = info.getEngine().getName();
        final String viewMode = info.getViewMode();
        for (final String filterName : renderer.getFilters()) {

            // Look for a filter for the current engine
            FilterType filterType = getFilterFor(engineName, filterName,
                    templateEngineName, viewMode);

            // Fall back to no specific engine
            if (filterType == null) {
                filterType = getFilterFor("*", filterName, templateEngineName,
                        viewMode);
            }

            if (filterType == null) {
                log.warn("Filter type '" + filterName + "' not found.");
                continue;
            }

            final Filter filter = filterType.getFilter();
            if (filter == null) {
                log.warn("Filter instantiation failed: " + filterName);
                continue;
            }

            final FilterTypeFamily filterTypeFamily = filterType.getFilterTypeFamily();

            if (filterTypeFamily == FilterTypeFamily.FORMAT) {
                final FormatType formatType = ((FormatFilter) filter).getFormatType();
                final Format format = ElementFormatter.getFormatByType(element,
                        formatType);
                if (format == null) {
                    log.debug("Could not find '" + formatType.getTypeName()
                            + "' format for: "
                            + element.getElementType().getTypeName());
                    continue;
                }
                info.setFormat(format);
            } else if (filterTypeFamily == FilterTypeFamily.STANDALONE) {
                // Do nothing
            } else {
                log.warn("Unsupported filter type: " + filterName);
            }

            info = filter.process(info, cache);

            // Abort the rendering if the filter returns null
            if (info == null) {
                break;
            }
        }
        return info;
    }

    private static FilterType getFilterFor(final String engineName,
            final String filterName, final String templateEngineName,
            final String viewMode) {

        // get the filter for this specified template engine and view mode
        FilterType filterType = (FilterType) typeRegistry.lookup(
                TypeFamily.FILTER, String.format("%s/%s/%s/%s", engineName,
                        templateEngineName, viewMode, filterName));

        // fall back to unspecified view mode
        if (filterType == null) {
            filterType = (FilterType) typeRegistry.lookup(TypeFamily.FILTER,
                    String.format("%s/%s/*/%s", engineName, templateEngineName,
                            filterName));
        }

        // fall back to unspecified template engine and view mode
        if (filterType == null) {
            filterType = (FilterType) typeRegistry.lookup(TypeFamily.FILTER,
                    String.format("%s/*/*/%s", engineName, filterName));
        }
        return filterType;
    }
}
