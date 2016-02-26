/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.layout.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;

/**
 * Helper to generate a layout xml output automatically from a schema definition.
 *
 * @author Anahide Tchertchian
 */
public class LayoutAutomaticGeneration {

    public static Document generateLayoutOutput(SchemaManager sm, String schemaName, boolean generateLabels) {
        String layoutName = schemaName;

        Document document = DocumentFactory.getInstance().createDocument();
        document.setName(layoutName);

        Element component = document.addElement("component");
        component.addAttribute("name", "myproject." + layoutName + ".generatedContrib");

        Element extension = component.addElement("extension");
        extension.addAttribute("target", "org.nuxeo.ecm.platform.forms.layout.WebLayoutManager");
        extension.addAttribute("point", "layouts");

        Element layout = extension.addElement("layout");
        layout.addAttribute("name", layoutName);
        Element rows = layout.addElement("rows");

        Schema schema = sm.getSchema(schemaName);
        String schemaPrefix = schema.getNamespace().prefix;

        List<Field> fields = new ArrayList<Field>();
        fields.addAll(schema.getFields());
        Collections.sort(fields, new Comparator<Field>() {
            public int compare(Field f1, Field f2) {
                return f1.getName().getLocalName().compareTo(f2.getName().getLocalName());
            }
        });

        for (Field field : fields) {
            // add row element
            Element row = rows.addElement("row");
            Element rowWidget = row.addElement("widget");
            String fieldName = field.getName().getLocalName();
            rowWidget.setText(fieldName);

            // add widget element
            boolean widgetResolved = false;
            if (field.getType().isSimpleType()) {
                boolean needsInputStyleClass = false;
                boolean needsDateFormat = false;
                Type fieldType = field.getType();
                String widgetType = null;
                if (fieldType == StringType.INSTANCE) {
                    widgetType = "text";
                    needsInputStyleClass = true;
                } else if (fieldType == LongType.INSTANCE || fieldType == IntegerType.INSTANCE
                        || fieldType == DoubleType.INSTANCE) {
                    widgetType = "int";
                    needsInputStyleClass = true;
                } else if (fieldType == DateType.INSTANCE) {
                    widgetType = "datetime";
                    needsDateFormat = true;
                } else {
                    break;
                }

                widgetResolved = true;

                Element widget = layout.addElement("widget");
                widget.addAttribute("name", fieldName);
                widget.addAttribute("type", widgetType);

                if (generateLabels) {
                    Element labels = widget.addElement("labels");
                    Element label = labels.addElement("label");
                    label.addAttribute("mode", BuiltinModes.ANY);
                    label.setText("label.widget." + layoutName + "." + fieldName);
                }

                Element fieldsElement = widget.addElement("fields");
                Element fieldElement = fieldsElement.addElement("field");
                if (schemaPrefix != null) {
                    fieldElement.setText(field.getName().getPrefixedName());
                } else {
                    fieldElement.addAttribute("schema", schemaName);
                    fieldElement.setText(fieldName);
                }

                // FIXME: this condition is always true. What's the point?
                if (needsDateFormat || needsInputStyleClass) {
                    Element properties = widget.addElement("properties");
                    if (needsDateFormat) {
                        properties.addAttribute("mode", BuiltinModes.ANY);
                        String defaultDatePattern = "#{nxu:basicDateFormatter()}";
                        Element patternProp = properties.addElement("property");
                        patternProp.addAttribute("name", "pattern");
                        patternProp.setText(defaultDatePattern);
                        Element formatProp = properties.addElement("property");
                        formatProp.addAttribute("name", "format");
                        formatProp.setText(defaultDatePattern);
                    }
                    if (needsInputStyleClass) {
                        properties.addAttribute("mode", BuiltinModes.EDIT);
                        String defaultStyleClass = "dataInputText";
                        Element styleClassProp = properties.addElement("property");
                        styleClassProp.addAttribute("name", "styleClass");
                        styleClassProp.setText(defaultStyleClass);
                    }
                }
            }

            if (!widgetResolved) {
                // widget needs to be done by hand for now
                layout.addComment("TODO: " + fieldName);
            }

        }

        return document;
    }
}
