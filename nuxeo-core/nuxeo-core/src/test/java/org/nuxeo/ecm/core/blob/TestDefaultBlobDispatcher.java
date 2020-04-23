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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.blob;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
public class TestDefaultBlobDispatcher {

    protected static final String DEFAULT = "default";

    protected static final String CUSTOM = "custom";

    protected DefaultBlobDispatcher dispatcherWith(String clause) {
        DefaultBlobDispatcher dispatcher = new DefaultBlobDispatcher();
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put(clause, CUSTOM);
        properties.put("default", DEFAULT); // NOSONAR
        dispatcher.initialize(properties);
        return dispatcher;
    }

    protected void expect(DefaultBlobDispatcher dispatcher, String expected, Object value) {
        Document doc = mock(Document.class);
        when(doc.getValue("prop")).thenReturn(value);
        assertEquals(expected, dispatcher.getProviderId(doc, null, null));
    }

    // ===== Null =====

    @Test
    public void testOperatorNullEq() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("prop=null");
        expect(dispatcher, CUSTOM, null);
        expect(dispatcher, DEFAULT, "foo");
        expect(dispatcher, DEFAULT, 555L);
    }

    @Test
    public void testOperatorNullNeq() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("prop!=null");
        expect(dispatcher, DEFAULT, null);
        expect(dispatcher, CUSTOM, "foo");
        expect(dispatcher, CUSTOM, 555L);
    }

    // ===== String =====

    @Test
    public void testOperatorStringEq() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("prop=foo");
        expect(dispatcher, DEFAULT, "bar");
        expect(dispatcher, CUSTOM, "foo");
    }

    @Test
    public void testOperatorStringNeq() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("prop!=foo");
        expect(dispatcher, CUSTOM, "bar");
        expect(dispatcher, DEFAULT, "foo");
    }

    @Test
    public void testOperatorStringLt() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("prop<foo");
        expect(dispatcher, CUSTOM, null);
        expect(dispatcher, CUSTOM, "bar");
        expect(dispatcher, DEFAULT, "foo");
        expect(dispatcher, DEFAULT, "gee");
    }

    @Test
    public void testOperatorStringLte() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("prop<=foo");
        expect(dispatcher, CUSTOM, null);
        expect(dispatcher, CUSTOM, "bar");
        expect(dispatcher, CUSTOM, "foo");
        expect(dispatcher, DEFAULT, "gee");
    }

    @Test
    public void testOperatorStringGt() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("prop>foo");
        expect(dispatcher, DEFAULT, null);
        expect(dispatcher, DEFAULT, "bar");
        expect(dispatcher, DEFAULT, "foo");
        expect(dispatcher, CUSTOM, "gee");
    }

    @Test
    public void testOperatorStringGte() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("prop>=foo");
        expect(dispatcher, DEFAULT, null);
        expect(dispatcher, DEFAULT, "bar");
        expect(dispatcher, CUSTOM, "foo");
        expect(dispatcher, CUSTOM, "gee");
    }

    @Test
    public void testOperatorStringGlob() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("prop~/fo?/bar*");
        expect(dispatcher, DEFAULT, null);
        expect(dispatcher, DEFAULT, "gee");
        expect(dispatcher, DEFAULT, "foo/bar");
        expect(dispatcher, DEFAULT, "/foo/ba");
        expect(dispatcher, DEFAULT, "/foo/baz");
        expect(dispatcher, CUSTOM, "/foo/bar");
        expect(dispatcher, CUSTOM, "/foo/bar/gee");
        expect(dispatcher, CUSTOM, "/fox/bar");
    }

    @Test
    public void testOperatorStringRegexp() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("prop^[fg]oo/?");
        expect(dispatcher, DEFAULT, null);
        expect(dispatcher, DEFAULT, "gee");
        expect(dispatcher, CUSTOM, "foo");
        expect(dispatcher, CUSTOM, "goo");
        expect(dispatcher, CUSTOM, "foo/");
        expect(dispatcher, CUSTOM, "goo/");
    }

    // ===== Boolean =====

    @Test
    public void testOperatorBooleanEq() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("prop=true");
        expect(dispatcher, DEFAULT, false);
        expect(dispatcher, CUSTOM, true);
        dispatcher = dispatcherWith("prop=foo");
        expect(dispatcher, DEFAULT, false);
        expect(dispatcher, DEFAULT, true);
    }

    @Test
    public void testOperatorBooleanNeq() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("prop!=true");
        expect(dispatcher, CUSTOM, false);
        expect(dispatcher, DEFAULT, true);
        dispatcher = dispatcherWith("prop!=foo");
        expect(dispatcher, CUSTOM, false);
        expect(dispatcher, CUSTOM, true);
    }

    // ===== Long =====

    @Test
    public void testOperatorLongEq() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("prop=555");
        expect(dispatcher, DEFAULT, 9L);
        expect(dispatcher, CUSTOM, 555L);
        dispatcher = dispatcherWith("prop=foo");
        expect(dispatcher, DEFAULT, 555L);
    }

    @Test
    public void testOperatorLongNeq() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("prop!=555");
        expect(dispatcher, CUSTOM, 9L);
        expect(dispatcher, DEFAULT, 555L);
        dispatcher = dispatcherWith("prop!=foo");
        expect(dispatcher, CUSTOM, 555L);
    }

    @Test
    public void testOperatorLongLt() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("prop<555");
        expect(dispatcher, CUSTOM, null); // treated as 0
        expect(dispatcher, CUSTOM, 9L); // to be sure we don't compare as strings
        expect(dispatcher, DEFAULT, 555L);
        expect(dispatcher, DEFAULT, 987L);
        dispatcher = dispatcherWith("prop<foo");
        expect(dispatcher, DEFAULT, 555L);
    }

    @Test
    public void testOperatorLongLte() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("prop<=555");
        expect(dispatcher, CUSTOM, null); // treated as 0
        expect(dispatcher, CUSTOM, 9L);
        expect(dispatcher, CUSTOM, 555L);
        expect(dispatcher, DEFAULT, 987L);
        dispatcher = dispatcherWith("prop<=foo");
        expect(dispatcher, DEFAULT, 555L);
    }

    @Test
    public void testOperatorLongGt() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("prop>555");
        expect(dispatcher, DEFAULT, null); // treated as 0
        expect(dispatcher, DEFAULT, 9L);
        expect(dispatcher, DEFAULT, 555L);
        expect(dispatcher, CUSTOM, 987L);
        dispatcher = dispatcherWith("prop>foo");
        expect(dispatcher, DEFAULT, 555L);
    }

    @Test
    public void testOperatorLongGte() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("prop>=555");
        expect(dispatcher, DEFAULT, null); // treated as 0
        expect(dispatcher, DEFAULT, 9L);
        expect(dispatcher, CUSTOM, 555L);
        expect(dispatcher, CUSTOM, 987L);
        dispatcher = dispatcherWith("prop>=foo");
        expect(dispatcher, DEFAULT, 555L);
    }

    // ===== Double =====

    @Test
    public void testOperatorDoubleEq() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("prop=555");
        expect(dispatcher, DEFAULT, 9.0D);
        expect(dispatcher, CUSTOM, 555.0D);
        dispatcher = dispatcherWith("prop=foo");
        expect(dispatcher, DEFAULT, 555.0D);
    }

    @Test
    public void testOperatorDoubleNeq() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("prop!=555");
        expect(dispatcher, CUSTOM, 9.0D);
        expect(dispatcher, DEFAULT, 555.0D);
        dispatcher = dispatcherWith("prop!=foo");
        expect(dispatcher, CUSTOM, 555.0D);
    }

    @Test
    public void testOperatorDoubleLt() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("prop<555");
        expect(dispatcher, CUSTOM, 9.0D); // to be sure we don't compare as strings
        expect(dispatcher, DEFAULT, 555.0D);
        expect(dispatcher, DEFAULT, 987.0D);
        dispatcher = dispatcherWith("prop<foo");
        expect(dispatcher, DEFAULT, 555.0D);
    }

    @Test
    public void testOperatorDoubleLte() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("prop<=555");
        expect(dispatcher, CUSTOM, 9.0D);
        expect(dispatcher, CUSTOM, 555.0D);
        expect(dispatcher, DEFAULT, 987.0D);
        dispatcher = dispatcherWith("prop<=foo");
        expect(dispatcher, DEFAULT, 555.0D);
    }

    @Test
    public void testOperatorDoubleGt() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("prop>555");
        expect(dispatcher, DEFAULT, 9.0D);
        expect(dispatcher, DEFAULT, 555.0D);
        expect(dispatcher, CUSTOM, 987.0D);
        dispatcher = dispatcherWith("prop>foo");
        expect(dispatcher, DEFAULT, 555.0D);
    }

    @Test
    public void testOperatorDoubleGte() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("prop>=555");
        expect(dispatcher, DEFAULT, 9.0D);
        expect(dispatcher, CUSTOM, 555.0D);
        expect(dispatcher, CUSTOM, 987.0D);
        dispatcher = dispatcherWith("prop>=foo");
        expect(dispatcher, DEFAULT, 555.0D);
    }

    // ===== Calendar =====

    protected static Calendar cal(String string) {
        ZonedDateTime instant = ZonedDateTime.ofInstant(Instant.parse(string), ZoneOffset.UTC);
        return GregorianCalendar.from(instant);
    }

    protected static final Calendar CAL1 = cal("2020-01-02T03:04:05Z");

    protected static final Calendar CAL2 = cal("2020-01-02T12:34:56Z");

    protected static final Calendar CAL3 = cal("2050-12-25T00:00:00Z");

    @Test
    public void testOperatorCalendarGlob() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("prop~2020-01-*");
        expect(dispatcher, CUSTOM, CAL1);
        expect(dispatcher, CUSTOM, CAL2);
        expect(dispatcher, DEFAULT, CAL3);
    }

    @Test
    public void testOperatorCalendarRegexp() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("prop^2020-01-.*");
        expect(dispatcher, CUSTOM, CAL1);
        expect(dispatcher, CUSTOM, CAL2);
        expect(dispatcher, DEFAULT, CAL3);
    }

    @Test
    public void testOperatorCalendarEq() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("prop=2020-01-02T12:34:56Z");
        expect(dispatcher, DEFAULT, CAL1);
        expect(dispatcher, CUSTOM, CAL2);
        dispatcher = dispatcherWith("prop=foo");
        expect(dispatcher, DEFAULT, CAL1);
    }

    @Test
    public void testOperatorCalendarNeq() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("prop!=2020-01-02T12:34:56Z");
        expect(dispatcher, CUSTOM, CAL1);
        expect(dispatcher, DEFAULT, CAL2);
        dispatcher = dispatcherWith("prop!=foo");
        expect(dispatcher, CUSTOM, CAL1);
    }

    @Test
    public void testOperatorCalendarLt() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("prop<2020-01-02T12:34:56Z");
        expect(dispatcher, CUSTOM, CAL1);
        expect(dispatcher, DEFAULT, CAL2);
        expect(dispatcher, DEFAULT, CAL3);
        dispatcher = dispatcherWith("prop<foo");
        expect(dispatcher, DEFAULT, CAL1);
    }

    @Test
    public void testOperatorCalendarLte() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("prop<=2020-01-02T12:34:56Z");
        expect(dispatcher, CUSTOM, CAL1);
        expect(dispatcher, CUSTOM, CAL2);
        expect(dispatcher, DEFAULT, CAL3);
        dispatcher = dispatcherWith("prop<=foo");
        expect(dispatcher, DEFAULT, CAL1);
    }

    @Test
    public void testOperatorCalendarGt() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("prop>2020-01-02T12:34:56Z");
        expect(dispatcher, DEFAULT, CAL1);
        expect(dispatcher, DEFAULT, CAL2);
        expect(dispatcher, CUSTOM, CAL3);
        dispatcher = dispatcherWith("prop>foo");
        expect(dispatcher, DEFAULT, CAL1);
    }

    @Test
    public void testOperatorCalendarGte() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("prop>=2020-01-02T12:34:56Z");
        expect(dispatcher, DEFAULT, CAL1);
        expect(dispatcher, CUSTOM, CAL2);
        expect(dispatcher, CUSTOM, CAL3);
        dispatcher = dispatcherWith("prop>=foo");
        expect(dispatcher, DEFAULT, CAL1);
    }

    // ===== Clauses =====

    @Test
    public void testClausesAnded() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("prop1=foo,prop2=bar");
        Document doc = mock(Document.class);

        when(doc.getValue("prop1")).thenReturn("foo");
        when(doc.getValue("prop2")).thenReturn("bar");
        assertEquals(CUSTOM, dispatcher.getProviderId(doc, null, null));

        when(doc.getValue("prop1")).thenReturn("foo");
        when(doc.getValue("prop2")).thenReturn("gee");
        assertEquals(DEFAULT, dispatcher.getProviderId(doc, null, null));

        when(doc.getValue("prop1")).thenReturn("gee");
        when(doc.getValue("prop2")).thenReturn("bar");
        assertEquals(DEFAULT, dispatcher.getProviderId(doc, null, null));
    }


    // ===== Names =====

    @Test
    public void testNameBlobName() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("blob:name=foo");
        Blob blob = mock(Blob.class);

        when(blob.getFilename()).thenReturn("bar");
        assertEquals(DEFAULT, dispatcher.getProviderId(null, blob, null));

        when(blob.getFilename()).thenReturn("foo");
        assertEquals(CUSTOM, dispatcher.getProviderId(null, blob, null));
    }

    @Test
    public void testNameBlobMimeType() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("blob:mime-type=foo");
        Blob blob = mock(Blob.class);

        when(blob.getMimeType()).thenReturn("bar");
        assertEquals(DEFAULT, dispatcher.getProviderId(null, blob, null));

        when(blob.getMimeType()).thenReturn("foo");
        assertEquals(CUSTOM, dispatcher.getProviderId(null, blob, null));
    }

    @Test
    public void testNameBlobEncoding() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("blob:encoding=foo");
        Blob blob = mock(Blob.class);

        when(blob.getEncoding()).thenReturn("bar");
        assertEquals(DEFAULT, dispatcher.getProviderId(null, blob, null));

        when(blob.getEncoding()).thenReturn("foo");
        assertEquals(CUSTOM, dispatcher.getProviderId(null, blob, null));
    }

    @Test
    public void testNameBlobDigest() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("blob:digest=foo");
        Blob blob = mock(Blob.class);

        when(blob.getDigest()).thenReturn("bar");
        assertEquals(DEFAULT, dispatcher.getProviderId(null, blob, null));

        when(blob.getDigest()).thenReturn("foo");
        assertEquals(CUSTOM, dispatcher.getProviderId(null, blob, null));
    }

    @Test
    public void testNameBlobLength() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("blob:length>500");
        Blob blob = mock(Blob.class);

        when(blob.getLength()).thenReturn(9L);
        assertEquals(DEFAULT, dispatcher.getProviderId(null, blob, null));

        when(blob.getLength()).thenReturn(555L);
        assertEquals(CUSTOM, dispatcher.getProviderId(null, blob, null));
    }

    @Test
    public void testNameBlobXpath() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("blob:xpath=foo");

        assertEquals(DEFAULT, dispatcher.getProviderId(null, null, "bar"));

        assertEquals(CUSTOM, dispatcher.getProviderId(null, null, "foo"));
    }

    @Test
    public void testNameEcmRepositoryName() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("ecm:repositoryName=foo");
        Document doc = mock(Document.class);

        when(doc.getRepositoryName()).thenReturn("bar");
        assertEquals(DEFAULT, dispatcher.getProviderId(doc, null, null));

        when(doc.getRepositoryName()).thenReturn("foo");
        assertEquals(CUSTOM, dispatcher.getProviderId(doc, null, null));
    }

    @Test
    public void testNameEcmPath() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("ecm:path=/foo");
        Document doc = mock(Document.class);

        when(doc.getPath()).thenReturn("/bar");
        assertEquals(DEFAULT, dispatcher.getProviderId(doc, null, null));

        when(doc.getPath()).thenReturn("/foo");
        assertEquals(CUSTOM, dispatcher.getProviderId(doc, null, null));
    }

    @Test
    public void testNameIsRecord() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("ecm:isRecord=true");
        Document doc = mock(Document.class);

        when(doc.isRecord()).thenReturn(false);
        assertEquals(DEFAULT, dispatcher.getProviderId(doc, null, null));

        when(doc.isRecord()).thenReturn(true);
        assertEquals(CUSTOM, dispatcher.getProviderId(doc, null, null));
    }

    @Test
    public void testNameRecords() {
        DefaultBlobDispatcher dispatcher = dispatcherWith("records");
        Document doc = mock(Document.class);

        when(doc.isRecord()).thenReturn(false);
        assertEquals(DEFAULT, dispatcher.getProviderId(doc, null, null));

        when(doc.isRecord()).thenReturn(true);
        assertEquals(DEFAULT, dispatcher.getProviderId(doc, null, null));

        when(doc.isRecord()).thenReturn(true);
        assertEquals(CUSTOM, dispatcher.getProviderId(doc, null, "content"));
    }

}
