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

package org.nuxeo.theme.webwidgets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WidgetFieldType {

    public String name;

    public String type;

    public String label;

    public String defaultValue = "";

    public String min = "0";

    public String max = "1";

    public String step = "1";

    public String onchange;

    public class Option {

        private String label;

        private String value;

        public Option(String label, String value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

    }

    public List<Option> options;

    public String getMax() {
        return max;
    }

    public void setMax(String max) {
        this.max = max;
    }

    public String getMin() {
        return min;
    }

    public void setMin(String min) {
        this.min = min;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getLabel() {
        return label;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOnchange() {
        return onchange;
    }

    public void setOnchange(String onchange) {
        this.onchange = onchange;
    }

    public List<Option> getOptions() {
        return options;
    }

    public void setOptions(List<Option> options) {
        this.options = options;
    }

    public Map<String, Object> getInfo() {
        final Map<String, Object> info = new HashMap<String, Object>();
        info.put("name", name);
        info.put("type", type);
        info.put("label", label);
        info.put("onchange", onchange);
        info.put("defaultValue", defaultValue);

        if (type.equals("range")) {
            info.put("min", min);
            info.put("max", max);
            info.put("step", step);
        }

        if (type.equals("list")) {
            final Map<String, String> optionsMap = new HashMap<String, String>();
            for (Option option : options) {
                optionsMap.put("label", option.getLabel());
                optionsMap.put("value", option.getValue());
            }
            info.put("options", optionsMap);
        }

        return info;
    }

}
