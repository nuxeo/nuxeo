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

package org.nuxeo.theme.editor.filters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.CellElement;
import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.elements.ElementFormatter;
import org.nuxeo.theme.elements.ElementType;
import org.nuxeo.theme.formats.Format;
import org.nuxeo.theme.formats.FormatType;
import org.nuxeo.theme.fragments.Fragment;
import org.nuxeo.theme.fragments.FragmentType;
import org.nuxeo.theme.html.Utils;
import org.nuxeo.theme.models.ModelType;
import org.nuxeo.theme.rendering.RenderingInfo;
import org.nuxeo.theme.rendering.StandaloneFilter;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.views.ViewType;

public class JSONModel extends StandaloneFilter {

    static final Pattern firstTagPattern = Pattern.compile("<(.*?)>",
            Pattern.DOTALL);

    static final Pattern otherTagsPattern = Pattern.compile("<.*?>(.*)",
            Pattern.DOTALL);

    static final String[] ALIGNMENTS = { "left", "center", "right" };

    @Override
    public RenderingInfo process(final RenderingInfo info, final boolean cache) {
        if (info.isRenderingPostponed(cache)) {
            return info;
        }
        final Element element = info.getElement();
        final String markup = info.getMarkup();
        final String templateEngine = info.getTemplateEngine().getName();
        final String viewMode = info.getViewMode();
        if (viewMode != null && viewMode.startsWith("area-styles")) {
            return info;
        }

        final Matcher firstMatcher = firstTagPattern.matcher(markup);
        final Matcher othersMatcher = otherTagsPattern.matcher(markup);

        if (!(firstMatcher.find() && othersMatcher.find())) {
            return info;
        }

        final Map<String, Object> model = new HashMap<String, Object>();
        final Map<String, Object> model_data = new HashMap<String, Object>();

        model_data.put("can add fragment", false);
        model_data.put("editable", false);
        model_data.put("duplicable", false);
        model_data.put("alignable", false);
        model_data.put("splittable", false);
        model_data.put("copyable", false);
        model_data.put("deletable", false);
        model_data.put("pastable", false);
        model_data.put("has widget", false);
        model_data.put("has style", false);
        model_data.put("has padding", false);

        if (element instanceof Fragment) {
            model_data.put("title", info.getElement().getDescription());
            model_data.put("can add fragment", true);
            model_data.put("editable", true);
            model_data.put("duplicable", true);
            model_data.put("copyable", true);
            model_data.put("deletable", true);
            model_data.put("pastable", true);

            if (info.getModel() != null) {
                List<Map<String, Object>> widgets = getWidgetsFor(element,
                        templateEngine);
                if (widgets.size() > 1) {
                    model_data.put("widgets", widgets);
                    model_data.put("has widget", true);
                }
            }
            model_data.put("has style", true);
        }

        else if (element instanceof CellElement) {
            model_data.put("title",
                    String.format("Cell %s", element.getOrder() + 1));
            if ("layout".equals(viewMode)) {
                model_data.put("splittable", true);
                model_data.put("deletable", true);
            }
            model_data.put("can add fragment", true);
            model_data.put("pastable", true);
            model_data.put("alignable", true);
            model_data.put("alignments", getAlignments());
            model_data.put("has padding", true);
        }

        model.put("id", String.format("%s-%s", info.getUid(), viewMode));
        model.put("type", "element");
        model.put("data", model_data);

        StringBuilder s = new StringBuilder();
        s.append('<');
        s.append(firstMatcher.group(1));
        s.append('>');
        s.append("<ins class=\"model\">");
        s.append(Utils.toJson(model));
        s.append("</ins>");
        s.append(othersMatcher.group(1));

        info.setMarkup(s.toString());
        return info;
    }

    private static List<Map<String, Object>> getWidgetsFor(
            final Element element, final String templateEngine) {
        final List<Map<String, Object>> widgets = new ArrayList<Map<String, Object>>();
        final FragmentType fragmentType = ((Fragment) element).getFragmentType();
        final ModelType modelType = fragmentType.getModelType();

        final FormatType widgetType = (FormatType) Manager.getTypeRegistry().lookup(
                TypeFamily.FORMAT, "widget");

        final Format widget = ElementFormatter.getFormatByType(element,
                widgetType);
        if (widget == null) {
            return widgets;
        }
        String thisViewName = widget.getName();

        for (Type v : Manager.getTypeRegistry().getTypes(TypeFamily.VIEW)) {
            final ViewType viewType = (ViewType) v;
            final String viewName = viewType.getViewName();

            if ("*".equals(viewName)) {
                continue;
            }

            // select the current template engine
            if (!templateEngine.equals(viewType.getTemplateEngine())) {
                continue;
            }

            // select fragment views
            ElementType elementType = viewType.getElementType();
            if (elementType != null
                    && !elementType.getTypeName().equals("fragment")) {
                continue;
            }

            // select widget view types
            if (!viewType.getFormatType().getTypeName().equals("widget")) {
                continue;
            }

            // match model types
            if (modelType == viewType.getModelType()) {
                Map<String, Object> widgetInfo = new HashMap<String, Object>();
                widgetInfo.put("choice", viewName);
                widgetInfo.put("label", viewName);
                if (thisViewName.equals(viewName)) {
                    widgetInfo.put("selected", true);
                }
                widgets.add(widgetInfo);
            }
        }
        return widgets;
    }

    private static List<Map<String, Object>> getAlignments() {
        final List<Map<String, Object>> alignments = new ArrayList<Map<String, Object>>();
        for (String position : ALIGNMENTS) {
            Map<String, Object> alignmentInfo = new HashMap<String, Object>();
            alignmentInfo.put("choice", position);
            alignmentInfo.put("label", position);
            alignmentInfo.put("icon", String.format(
                    "/skin/nxthemes-editor/img/align-%s-16.png", position));
            alignments.add(alignmentInfo);
        }
        return alignments;
    }

}
