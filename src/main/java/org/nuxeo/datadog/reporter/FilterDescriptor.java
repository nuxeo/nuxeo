/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.datadog.reporter;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import org.coursera.metrics.datadog.DatadogReporter.Expansion;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

import com.google.common.collect.ImmutableSet;

/**
 * @since 10.1
 */
@XObject("filter")
public class FilterDescriptor {

    @XNode("@method")
    protected String method;

    @XNodeList(value = "includes/include", type = ArrayList.class, componentType = String.class)
    protected List<String> includes;

    @XNodeList(value = "excludes/exclude", type = ArrayList.class, componentType = String.class)
    protected List<String> excludes;

    @XNodeList(value = "expansions/expansion", type = ArrayList.class, componentType = String.class)
    protected List<String> expansions;

    public boolean getUseSubstringMatching() {
        return "substring".equals(method);
    }

    public boolean getUseRegexFilters() {
        return "regex".equals(method);
    }

    public ImmutableSet<String> getIncludes() {
        return ImmutableSet.<String> builder().addAll(includes).build();
    }

    public ImmutableSet<String> getExcludes() {
        return ImmutableSet.<String> builder().addAll(excludes).build();
    }

    public EnumSet<Expansion> getExpansions() {
        if (expansions.isEmpty()) {
            return Expansion.ALL;
        } else {
            return Expansion.ALL.stream().filter(e -> expansions.contains(e.toString())).collect(
                    Collectors.toCollection(() -> EnumSet.noneOf(Expansion.class)));
        }
    }
}
