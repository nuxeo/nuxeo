/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.search.backend.compass.lucene;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermPositions;
import org.apache.lucene.search.Scorer;

public class TestMatchBeforeQuery extends TestCase {

    /**
     * To mimick a TermPositions entry.
     * The important fact for tests is that
     * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
     *
     */
    class TP {
        private final int doc;
        private final int pos;
        private boolean used;

        TP(int doc, int pos) {
            this.doc = doc;
            this.pos = pos;
            used = false;
        }

        public int nextPosition() {
            if (used) {
                throw new RuntimeException("Already used !");
            }
            used = true;
            return pos;
        }

        public int doc() {
            return doc;
        }
    }


    /**
     * To mimick TermPositions.
     *
     * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
     *
     */
    class TPS implements TermPositions {

        private final Iterator<TP> tpsit;
        private TP current;

        TPS(List<TP> items) {
            tpsit = items.iterator();
        }

        public int nextPosition() throws IOException {
            return current.nextPosition();
        }

        public void close() throws IOException {

        }

        public int doc() {
            return current.doc();
        }

        public int freq() {
            return 1; // our TP objects have only one position
        }

        public boolean next() throws IOException {
            if (tpsit.hasNext()) {
                current = tpsit.next();
                return true;
            }
            return false;
        }

        public int read(int[] arg0, int[] arg1) throws IOException {
            // Auto-generated method stub
            return 0;
        }

        public void seek(Term arg0) throws IOException {
            // Auto-generated method stub
        }

        public void seek(TermEnum arg0) throws IOException {
            // Auto-generated method stub
        }

        public boolean skipTo(int i) throws IOException {
            if (current == null) {
                next();
            }
            while (doc() < i && next()) {}
            return doc() >= i;
        }

        public byte[] getPayload(byte[] arg0, int arg1) throws IOException {
            // TODO Auto-generated method stub
            return null;
        }

        public int getPayloadLength() {
            // TODO Auto-generated method stub
            return 0;
        }

        public boolean isPayloadAvailable() {
            // TODO Auto-generated method stub
            return false;
        }
    }

    private Map<String, TPS> occurrences;

    /**
     * Factors out MatchBeforeScorer instantiation.
     *
     * @param required List of required term values
     * @param excluded List of excluded term values
     * @return ready to work Scorer instance
     */
    Scorer makeScorer(List<String> required, List<String> excluded) {
        int i;

        TPS[] allowedTps = new TPS[required.size()];
        i = 0;
        for (String req : required) {
            allowedTps[i++] = occurrences.get(req);
        }

        TPS[] excludedTps = new TPS[excluded.size()];
        i = 0;
        for (String exc : excluded) {
            excludedTps[i++] = occurrences.get(exc);
        }

        // At the time being, what is passed to query
        // constructor doesn't matter
        MatchBeforeQuery query = new MatchBeforeQuery("field",
                required, excluded);
        return query.new MatchBeforeScorer(allowedTps,
                excludedTps, null);

    }

    @Override
    public void setUp() throws Exception {
        // Document 0: A,X,B
        // Document 1: B,Y,A
        // Document 2: Y,B,X
        // Document 3: B
        // We'll use X,Y as excluded, A,B required

        occurrences = new HashMap<String, TPS>();
        occurrences.put("A", new TPS(Arrays.asList(new TP(0, 0), new TP(1, 2))));
        occurrences.put("B", new TPS(Arrays.asList(new TP(0, 2),
                new TP(1, 0), new TP(2, 1), new TP(3, 0))));
        occurrences.put("X", new TPS(Arrays.asList(new TP(0, 1), new TP(2, 2))));
        occurrences.put("Y", new TPS(Arrays.asList(new TP(1, 1), new TP(2, 0))));
    }

    public void testTPS() throws IOException {
        TPS tps;

        tps = occurrences.get("X");
        assertTrue(tps.skipTo(2));
        assertEquals(2, tps.doc());
    }

    public void testNext1() throws Exception {
        Scorer scorer = makeScorer(Arrays.asList("A"), Arrays.asList("X", "Y"));

        assertTrue(scorer.next());
        assertEquals(0, scorer.doc());
        assertFalse(scorer.next());
    }

    public void testSkipTo1() throws Exception {
        Scorer scorer = makeScorer(Arrays.asList("A"), Arrays.asList("X", "Y"));

        assertTrue(scorer.skipTo(0));
        assertEquals(0, scorer.doc());
        assertFalse(scorer.next());
    }

    public void testNext2() throws Exception {
        Scorer scorer = makeScorer(Arrays.asList("B"), Arrays.asList("X", "Y"));

        assertTrue(scorer.next());
        assertEquals(1, scorer.doc());

        assertTrue(scorer.next());
        assertEquals(3, scorer.doc());

        assertFalse(scorer.next());
    }

    public void testSkipTo2() throws Exception {
        Scorer scorer = makeScorer(Arrays.asList("B"), Arrays.asList("X", "Y"));

        assertTrue(scorer.skipTo(2));
        assertEquals(3, scorer.doc());

        assertFalse(scorer.next());
    }

    public void testNext4() throws Exception {
        Scorer scorer = makeScorer(Arrays.asList("A", "B"), Arrays.asList("X"));

        assertTrue(scorer.next());
        assertEquals(0, scorer.doc());

        assertTrue(scorer.next());
        assertEquals(1, scorer.doc());

        assertTrue(scorer.next());
        assertEquals(2, scorer.doc());

        assertTrue(scorer.next());
        assertEquals(3, scorer.doc());

        assertFalse(scorer.next());
    }

    public void testSkipTo4() throws Exception {
        Scorer scorer = makeScorer(Arrays.asList("A", "B"), Arrays.asList("X"));

        assertTrue(scorer.skipTo(2));
        assertEquals(2, scorer.doc());

        assertTrue(scorer.next());
        assertEquals(3, scorer.doc());

        assertFalse(scorer.next());
    }

    public void testNext5() throws Exception {
        Scorer scorer = makeScorer(Arrays.asList("A", "B"), Arrays.asList("Y"));

        assertTrue(scorer.next());
        assertEquals(0, scorer.doc());

        assertTrue(scorer.next());
        assertEquals(1, scorer.doc());

        assertTrue(scorer.next());
        assertEquals(3, scorer.doc());

        assertFalse(scorer.next());
    }

    public void testSkipTo5() throws Exception {
        Scorer scorer = makeScorer(Arrays.asList("A", "B"), Arrays.asList("Y"));

        assertTrue(scorer.skipTo(1));
        assertEquals(1, scorer.doc());
    }

    public void testNext6() throws Exception {
        Scorer scorer = makeScorer(Arrays.asList("A"), Arrays.asList("Y"));

        assertTrue(scorer.next());
        assertEquals(0, scorer.doc());

        assertFalse(scorer.next());
    }

    public void testSkipTo6() throws Exception {
        Scorer scorer = makeScorer(Arrays.asList("A"), Arrays.asList("Y"));

        assertFalse(scorer.skipTo(1));
    }

    public void testNext7() throws Exception {
        Scorer scorer = makeScorer(Arrays.asList("B"), Arrays.asList("X"));

        assertTrue(scorer.next());
        assertEquals(1, scorer.doc());

        assertTrue(scorer.next());
        assertEquals(2, scorer.doc());

        assertTrue(scorer.next());
        assertEquals(3, scorer.doc());

        assertFalse(scorer.next());
    }

    public void testNext8() throws Exception {
        Scorer scorer = makeScorer(Arrays.asList("B"), Arrays.asList("Y"));

        assertTrue(scorer.next());
        assertEquals(0, scorer.doc());

        assertTrue(scorer.next());
        assertEquals(1, scorer.doc());

        assertTrue(scorer.next());
        assertEquals(3, scorer.doc());

        assertFalse(scorer.next());
    }

    public void testNext9() throws Exception {
        Scorer scorer = makeScorer(Arrays.asList("A"), Arrays.asList("X"));

        assertTrue(scorer.next());
        assertEquals(0, scorer.doc());

        assertTrue(scorer.next());
        assertEquals(1, scorer.doc());

        assertFalse(scorer.next());
    }

    @SuppressWarnings("unchecked")
    public void testNextA() throws Exception {
        Scorer scorer = makeScorer(Arrays.asList("A"), Collections.EMPTY_LIST);

        assertTrue(scorer.next());
        assertEquals(0, scorer.doc());

        assertTrue(scorer.next());
        assertEquals(1, scorer.doc());

        assertFalse(scorer.next());
    }

    @SuppressWarnings("unchecked")
    public void testNextB() throws Exception {
        Scorer scorer = makeScorer(Arrays.asList("B"), Collections.EMPTY_LIST);

        assertTrue(scorer.next());
        assertEquals(0, scorer.doc());

        assertTrue(scorer.next());
        assertEquals(1, scorer.doc());

        assertTrue(scorer.next());
        assertEquals(2, scorer.doc());

        assertTrue(scorer.next());
        assertEquals(3, scorer.doc());

        assertFalse(scorer.next());
    }

}
