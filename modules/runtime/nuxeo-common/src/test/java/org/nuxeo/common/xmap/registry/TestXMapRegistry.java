/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.common.xmap.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.XMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @since 11.5
 */
public class TestXMapRegistry {

    protected static Context ctx = new Context();

    protected Element load(String resource) throws Exception {
        URL url = Thread.currentThread().getContextClassLoader().getResource("registry/" + resource);
        DocumentBuilderFactory factory = XMap.getFactory();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(url.openStream());
        return document.getDocumentElement();
    }

    protected void checkSample(MapRegistry mreg, String name, String value, Boolean bool, List<String> stringList,
            List<String> stringListAnnotated, Map<String, String> stringMap, Map<String, String> stringMapAnnotated) {
        Optional<SampleDescriptor> desc = mreg.getContribution(name);
        assertTrue(desc.isPresent());
        assertEquals(name, desc.get().name);
        assertEquals(value, desc.get().value);
        assertEquals(bool, desc.get().bool);
        assertEquals(stringList, desc.get().stringList);
        assertEquals(stringListAnnotated, desc.get().stringListAnnotated);
        assertEquals(stringMap, desc.get().stringMap);
        assertEquals(stringMapAnnotated, desc.get().stringMapAnnotated);
    }

    protected void checkSampleInitialStatus(MapRegistry mreg) {
        assertEquals(5, mreg.getContributions().size());
        assertEquals(5, mreg.getContributionValues().size());
        assertEquals(List.of("sample1", "sample2", "sample3", "sample4", "sample5"),
                new ArrayList<>(mreg.getContributions().keySet()));
        assertFalse(mreg.getContribution("foo").isPresent());
        checkSample(mreg, "sample1", "Sample 1 Value", false, List.of(), null, Map.of(), Map.of());
        checkSample(mreg, "sample2", "Sample 2 Value", true, List.of("sample2 - item1", "sample2 - item2"),
                List.of("sample2 - annotated item1", "sample2 - annotated item2"),
                Map.of("item1", "sample2 - item1", "item2", "sample2 - item2"),
                Map.of("item1", "sample2 - annotated item1", "item2", "sample2 - annotated item2"));
        checkSample(mreg, "sample3", "Sample 3 Value", true, List.of("sample3 - item1", "sample3 - item2"),
                List.of("sample3 - annotated item1", "sample3 - annotated item2"),
                Map.of("item1", "sample3 - item1", "item2", "sample3 - item2"),
                Map.of("item1", "sample3 - annotated item1", "item2", "sample3 - annotated item2"));
        checkSample(mreg, "sample4", "Sample 4 Value", true, List.of("sample4 - item1", "sample4 - item2"),
                List.of("sample4 - annotated item1", "sample4 - annotated item2"),
                Map.of("item1", "sample4 - item1", "item2", "sample4 - item2"),
                Map.of("item1", "sample4 - annotated item1", "item2", "sample4 - annotated item2"));
        checkSample(mreg, "sample5", "Sample", true, List.of("sample5 - item1", "sample5 - item2"),
                List.of("sample5 - annotated item1", "sample5 - annotated item2"),
                Map.of("item1", "sample5 - item1", "item2", "sample5 - item2"),
                Map.of("item1", "sample5 - annotated item1", "item2", "sample5 - annotated item2"));
    }

    protected void checkSomeMergeStatus(MapRegistry mreg, int nb) {
        assertEquals(nb, mreg.getContributions().size());
        assertEquals(nb, mreg.getContributionValues().size());
        // annotated list and map overridden, others merged
        checkSample(mreg, "sample3", "Sample 3 Overridden", true,
                List.of("sample3 - item1", "sample3 - item2", "sample3 - item1 overridden"),
                List.of("sample3 - annotated item1 overridden"),
                Map.of("item1", "sample3 - item1 overridden", "item2", "sample3 - item2", "item3",
                        "sample3 - item3 overridden"),
                Map.of("item1", "sample3 - annotated item1 overridden", "item3",
                        "sample3 - annotated item3 overridden"));
        // all merged
        checkSample(mreg, "sample4", "Sample 4 Merged", true,
                List.of("sample4 - item1", "sample4 - item2", "sample4 - item1 merged"),
                List.of("sample4 - annotated item1", "sample4 - annotated item2", "sample4 - annotated item1 merged"),
                Map.of("item1", "sample4 - item1 merged", "item2", "sample4 - item2", "item3",
                        "sample4 - item3 merged"),
                Map.of("item1", "sample4 - annotated item1 merged", "item2", "sample4 - annotated item2", "item3",
                        "sample4 - annotated item3 merged"));
        // all merged except annotated map, not merging by default
        checkSample(mreg, "sample5", "Sample 5 Implicit Merge", true,
                List.of("sample5 - item1", "sample5 - item2", "sample5 - item1 added"),
                List.of("sample5 - annotated item1", "sample5 - annotated item2", "sample5 - annotated item1 added"),
                Map.of("item1", "sample5 - item1 added", "item2", "sample5 - item2", "item3", "sample5 - item3 added"),
                Map.of("item1", "sample5 - annotated item1 added", "item3", "sample5 - annotated item3 added"));
        checkSample(mreg, "sample6", "Sample 6 Value", true, List.of(), null, Map.of(), Map.of());
    }

    @Test
    public void testSampleDescriptor() throws Exception {
        XMap xmap = new XMap();
        XAnnotatedObject xob = xmap.register(SampleDescriptor.class);
        Registry registry = xmap.getRegistry(xob);
        assertNotNull(registry);
        xmap.register(registry, ctx, load("sample-1.xml"), "sample-1");
        assertTrue(registry instanceof MapRegistry);
        MapRegistry mreg = (MapRegistry) registry;
        checkSampleInitialStatus(mreg);

        // check merge
        xmap.register(registry, ctx, load("sample-2.xml"), "sample-2");

        assertEquals(6, mreg.getContributions().size());
        // "value" overridden
        // "stringList" added
        checkSample(mreg, "sample1", "Sample 1 Additions", false, List.of("sample1 - item1 added"),
                List.of("sample1 - annotated item1 added"), Map.of("item1", "sample1 - item1 added"),
                Map.of("item1", "sample1 - annotated item1 added"));
        // "value" emptied (does not go back to default value "Sample")
        // annotated lists and maps emptied, others merged
        checkSample(mreg, "sample2", "", true, List.of("sample2 - item1", "sample2 - item2"), null,
                Map.of("item1", "sample2 - item1", "item2", "sample2 - item2"), Map.of());
        checkSomeMergeStatus(mreg, 6);

        // check unregister
        xmap.unregister(registry, "sample-2");
        checkSampleInitialStatus(mreg);

        // continue checking merge again
        xmap.register(registry, ctx, load("sample-2.xml"), "sample-2");
        xmap.register(registry, ctx, load("sample-3.xml"), "sample-3");

        assertEquals(4, mreg.getContributions().size());
        // sample 1 disabled
        assertFalse(mreg.getContribution("sample1").isPresent());
        // sample 2 removed
        assertFalse(mreg.getContribution("sample2").isPresent());
        // no change for others
        checkSomeMergeStatus(mreg, 4);

        // check merge again
        xmap.register(registry, ctx, load("sample-4.xml"), "sample-4");

        assertEquals(6, mreg.getContributions().size());
        // sample 1 re-enabled, old values have been kept
        checkSample(mreg, "sample1", "Sample 1 Re-enabled", false, List.of("sample1 - item1 added"),
                List.of("sample1 - annotated item1 added"), Map.of("item1", "sample1 - item1 added"),
                Map.of("item1", "sample1 - annotated item1 added"));
        // sample 2 re-added, with empty values
        checkSample(mreg, "sample2", "Sample 2 Re-added", true, List.of(), null, Map.of(), Map.of());
        // no change for others
        checkSomeMergeStatus(mreg, 6);
    }

    protected void checkSampleEnable(MapRegistry mreg, String name, String value, Boolean bool, Boolean activated) {
        Optional<SampleEnableDescriptor> desc = mreg.getContribution(name);
        assertTrue(desc.isPresent());
        assertEquals(name, desc.get().name);
        assertEquals(value, desc.get().value);
        assertEquals(bool, desc.get().bool);
        assertEquals(activated, desc.get().activated);
    }

    @Test
    public void testSampleEnabledDescriptor() throws Exception {
        XMap xmap = new XMap();
        XAnnotatedObject xob = xmap.register(SampleEnableDescriptor.class);
        Registry registry = xmap.getRegistry(xob);
        assertNotNull(registry);
        xmap.register(registry, ctx, load("sample-common-1.xml"), "sample-common-1");
        assertTrue(registry instanceof MapRegistry);
        MapRegistry mreg = (MapRegistry) registry;
        assertEquals(4, mreg.getContributions().size());
        checkSampleEnable(mreg, "sample1", "Sample 1 Value", null, null);
        checkSampleEnable(mreg, "sample2", "Sample 2 Value", null, null);
        checkSampleEnable(mreg, "sample3", "Sample 3 Value", null, null);
        checkSampleEnable(mreg, "sample4", "Sample 4 Value", null, null);

        xmap.register(registry, ctx, load("sample-common-2.xml"), "sample-common-2");
        // sample1 and sample2 disabled, sample4 removed
        assertEquals(1, mreg.getContributions().size());
        checkSampleEnable(mreg, "sample3", "Sample 3 Additions", true, null);

        xmap.register(registry, ctx, load("sample-common-3.xml"), "sample-common-3");
        assertEquals(3, mreg.getContributions().size());
        // "value" and "bool" overridden + activated
        checkSampleEnable(mreg, "sample1", "Sample 1 Additions", true, true);
        checkSampleEnable(mreg, "sample2", "Sample 2 Overridden", true, true);
        // "override" ignored
        checkSampleEnable(mreg, "sample3", "Sample 3 Overridden", true, null);
    }

    protected void checkSampleNoMerge(MapRegistry mreg, String name, String value, Boolean bool) {
        Optional<SampleNoMergeDescriptor> desc = mreg.getContribution(name);
        assertTrue(desc.isPresent());
        assertEquals(name, desc.get().name);
        assertEquals(value, desc.get().value);
        assertEquals(bool, desc.get().bool);
    }

    @Test
    public void testSampleNoMergeDescriptor() throws Exception {
        XMap xmap = new XMap();
        XAnnotatedObject xob = xmap.register(SampleNoMergeDescriptor.class);
        Registry registry = xmap.getRegistry(xob);
        assertNotNull(registry);
        xmap.register(registry, ctx, load("sample-common-1.xml"), "sample-common-1");
        assertTrue(registry instanceof MapRegistry);
        MapRegistry mreg = (MapRegistry) registry;
        assertEquals(4, mreg.getContributions().size());
        checkSampleNoMerge(mreg, "sample1", "Sample 1 Value", null);
        checkSampleNoMerge(mreg, "sample2", "Sample 2 Value", null);
        checkSampleNoMerge(mreg, "sample3", "Sample 3 Value", null);
        checkSampleNoMerge(mreg, "sample4", "Sample 4 Value", null);

        xmap.register(registry, ctx, load("sample-common-2.xml"), "sample-common-2");
        // sample1 and sample2 disablement ignored, sample4 removed
        assertEquals(3, mreg.getContributions().size());
        checkSampleNoMerge(mreg, "sample1", "Sample 1 Additions", true);
        checkSampleNoMerge(mreg, "sample2", "Sample 2 Additions", true);
        checkSampleNoMerge(mreg, "sample3", "Sample 3 Additions", true);

        xmap.register(registry, ctx, load("sample-common-3.xml"), "sample-common-3");
        assertEquals(3, mreg.getContributions().size());
        // all overridden
        checkSampleNoMerge(mreg, "sample1", null, null);
        checkSampleNoMerge(mreg, "sample2", "Sample 2 Overridden", null);
        checkSampleNoMerge(mreg, "sample3", "Sample 3 Overridden", null);
    }

    protected void checkSampleOverride(MapRegistry mreg, String name, String value, Boolean bool) {
        Optional<SampleOverrideDescriptor> desc = mreg.getContribution(name);
        assertTrue(desc.isPresent());
        assertEquals(name, desc.get().name);
        assertEquals(value, desc.get().value);
        assertEquals(bool, desc.get().bool);
    }

    @Test
    public void testSampleOverrideDescriptor() throws Exception {
        XMap xmap = new XMap();
        XAnnotatedObject xob = xmap.register(SampleOverrideDescriptor.class);
        Registry registry = xmap.getRegistry(xob);
        assertNotNull(registry);
        xmap.register(registry, ctx, load("sample-common-1.xml"), "sample-common-1");
        assertTrue(registry instanceof MapRegistry);
        MapRegistry mreg = (MapRegistry) registry;
        assertEquals(4, mreg.getContributions().size());
        checkSampleOverride(mreg, "sample1", "Sample 1 Value", null);
        checkSampleOverride(mreg, "sample2", "Sample 2 Value", null);
        checkSampleOverride(mreg, "sample3", "Sample 3 Value", null);
        checkSampleOverride(mreg, "sample4", "Sample 4 Value", null);

        xmap.register(registry, ctx, load("sample-common-2.xml"), "sample-common-2");
        // sample1 and sample2 disablement ignored
        assertEquals(4, mreg.getContributions().size());
        checkSampleOverride(mreg, "sample1", "Sample 1 Additions", true);
        checkSampleOverride(mreg, "sample2", "Sample 2 Additions", true);
        checkSampleOverride(mreg, "sample3", "Sample 3 Additions", true);
        // sample4 removal ignored + override
        checkSampleOverride(mreg, "sample4", null, null);

        xmap.register(registry, ctx, load("sample-common-3.xml"), "sample-common-3");
        assertEquals(4, mreg.getContributions().size());
        // all overridden except sample3, merged
        checkSampleOverride(mreg, "sample1", null, null);
        checkSampleOverride(mreg, "sample2", "Sample 2 Overridden", null);
        checkSampleOverride(mreg, "sample3", "Sample 3 Overridden", true);
        checkSampleOverride(mreg, "sample4", null, null);
    }

    protected void checkSampleSingle(SingleRegistry sreg, String name, String value) {
        Optional<SampleSingleDescriptor> desc = sreg.getContribution();
        assertTrue(desc.isPresent());
        assertEquals(name, desc.get().name);
        assertEquals(value, desc.get().value);
    }

    @Test
    public void testSampleSingleDescriptor() throws Exception {
        XMap xmap = new XMap();
        XAnnotatedObject xob = xmap.register(SampleSingleDescriptor.class);
        Registry registry = xmap.getRegistry(xob);
        assertNotNull(registry);
        xmap.register(registry, ctx, load("sample-single-1.xml"), "sample-single-1");
        assertTrue(registry instanceof SingleRegistry);
        SingleRegistry sreg = (SingleRegistry) registry;
        checkSampleSingle(sreg, "sample1", "Sample 1 Value");

        xmap.register(registry, ctx, load("sample-single-2.xml"), "sample-single-2");
        // disabled
        assertFalse(sreg.getContribution().isPresent());

        xmap.register(registry, ctx, load("sample-single-3.xml"), "sample-single-3");
        // "value" overridden
        checkSampleSingle(sreg, "sample1", "Sample 1 Overridden");
    }

}
