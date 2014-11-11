/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: ResultSet.java 14558 2007-03-23 13:14:38Z gracinet $
 */

package org.nuxeo.ecm.core.search.backend.compass.lucene;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermPositions;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Weight;

/**
 * Simple proof of concept for writing a custom query.
 * <p>
 * Matching documents are those that have the term at a given exact position
 * This is not useful for the compass backend per se, but it might help
 * migrations to future versions of Lucene, as this is simpler than
 * the query we use for Nuxeo's ACP.
 * <p>
 * Tested by TestCompassBackendInternals.
 *
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 *
 */

 public class ProofConceptQuery extends Query {

    private static final long serialVersionUID = -2516356860500766401L;
    private final Term term;
    private final int pos;

    public ProofConceptQuery(Term term, int pos) {
        this.term = term;
        this.pos = pos;
    }

    @Override
    public String toString(String arg0) {
        return String.format("%s:ProofConcept(%s, %d)", term.field(), term.text(), pos);
    }

    private class ProofWeight implements Weight {

        private static final long serialVersionUID = 352257796126743536L;

        private final Similarity similarity;

        ProofWeight(Searcher searcher) {
            similarity = getSimilarity(searcher);
        }

        public Explanation explain(IndexReader arg0, int arg1)
                throws IOException {
            // TODO Auto-generated method stub
            return null;
        }

        public Query getQuery() {
            // TODO Auto-generated method stub
            return ProofConceptQuery.this;
        }

        public float getValue() {
            // TODO Auto-generated method stub
            return (float) 1.0;
        }

        public void normalize(float arg0) {
            // TODO Auto-generated method stub
        }

        public Scorer scorer(IndexReader reader) throws IOException {
            TermPositions tp = reader.termPositions(term);
            return new ProofScorer(tp, pos, similarity);
        }

        public float sumOfSquaredWeights() throws IOException {
            // TODO Auto-generated method stub
            return (float) 1.0;
        }
    }

    protected Weight createWeight(Searcher searcher) throws IOException {
        return new ProofWeight(searcher);
    }


    private static class ProofScorer extends Scorer {

        private int doc; // Current matching document
        private final TermPositions tp; // Matching Term Positions
        private final int pos; // Wanted position

        ProofScorer(TermPositions tp, int pos, Similarity similarity) {
            super(similarity);
            this.tp = tp;
            this.pos = pos;
        }

        @Override
        public int doc() {
            return doc;
        }

        @Override
        public Explanation explain(int arg0) throws IOException {
            // TODO Auto-generated method stub
            return null;
        }

        private boolean currentTermPosMatches() throws IOException {
            for (int i = 0; i < tp.freq(); i++) {
                if (pos == tp.nextPosition()) {
                    doc = tp.doc();
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean next() throws IOException {
            while (tp.next()) {
                if (currentTermPosMatches()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public float score() throws IOException {
            // GR. If I understand what the java doc says, calling score is
            // implying that we have a matching document.
            return (float) 1.0;
        }

        @Override
        public boolean skipTo(int minDoc) throws IOException {
            if (!tp.skipTo(minDoc)) {
                return false;
            }
            if (currentTermPosMatches()) {
                return true;
            }
            return next();
        }
    }

}
