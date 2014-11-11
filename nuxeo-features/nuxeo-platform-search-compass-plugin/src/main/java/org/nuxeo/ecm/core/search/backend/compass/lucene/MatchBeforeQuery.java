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
import java.util.List;

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
 * This Query takes two sets of terms: required and excluded Documents match iff
 * required term occurs and no excluded one occurs <strong>before</strong> in
 * the checked field.
 * <p>
 * The sets of terms can be initialized as lists, to allow optimization by
 * frequency guesses.
 * <p>
 * Typical use-case is to search on ACLs. For this use-case, the required terms
 * would be encodings of all granting expressions that have effect on the user,
 * while the excluded would be the denying expressions.
 *
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 */
public class MatchBeforeQuery extends Query {

    private static final long serialVersionUID = 1L;

    private final List<String> required;

    private final List<String> excluded;

    private final String field;

    public MatchBeforeQuery(String field, List<String> allowings,
            List<String> forbiddings) {
        this.field = field;
        required = allowings;
        excluded = forbiddings;
    }

    @Override
    public String toString(String arg0) {
        return String.format("%s:MatchBefore(%s, %s)", field,
                required.toString(), excluded.toString());
    }

    private class MatchBeforeWeight implements Weight {
        private static final long serialVersionUID = -5665415141691969805L;

        private final Similarity similarity;

        MatchBeforeWeight(Searcher searcher) {
            similarity = getSimilarity(searcher);
        }

        public Explanation explain(IndexReader arg0, int arg1)
                throws IOException {
            // TODO Auto-generated method stub
            return null;
        }

        public Query getQuery() {
            return MatchBeforeQuery.this;
        }

        public float getValue() {
            // TODO (almost) Auto-generated method stub
            return (float) 1.0;
        }

        public void normalize(float arg0) {
            // TODO Auto-generated method stub
        }

        public Scorer scorer(IndexReader reader) throws IOException {
            TermPositions[] requiredTps = new TermPositions[required.size()];
            int i = 0;
            for (String req : required) {
                requiredTps[i++] = reader.termPositions(new Term(field, req));
            }
            TermPositions[] excludedTps = new TermPositions[excluded.size()];
            i = 0;
            for (String exc : excluded) {
                excludedTps[i++] = reader.termPositions(new Term(field, exc));
            }
            return new MatchBeforeScorer(requiredTps, excludedTps, similarity);
        }

        public float sumOfSquaredWeights() throws IOException {
            // TODO Auto-generated method stub
            return (float) 1.0;
        }
    }

    @Override
    protected Weight createWeight(Searcher searcher) throws IOException {
        return new MatchBeforeWeight(searcher);
    }

    class MatchBeforeScorer extends Scorer {

        private final TermPositions[] requiredTps; // Occurrences of required terms

        private final TermPositions[] excludedTps; // Occurrences of excluded terms

        private int doc; // Current matching document

        // Current information we have on heads of iterators for required terms
        // array of pairs (doc, position of first occurrence)
        final int[][] currentRequired;

        final int[][] currentExcluded;

        private final int reqLen;

        private final int excLen;

        MatchBeforeScorer(TermPositions[] requiredTps,
                TermPositions[] excludedTps, Similarity similarity) {
            super(similarity);
            // it is stated in Lucene doc that doc numbers are nonnegative
            doc = -1; // needed by skipTo so that skipTo(0) can work
            this.requiredTps = requiredTps;
            this.excludedTps = excludedTps;
            reqLen = requiredTps.length;
            excLen = excludedTps.length;

            currentRequired = new int[reqLen][2];
            for (int i = 0; i < reqLen; i++) {
                currentRequired[i][0] = -1; // will trigger loads
            }
            currentExcluded = new int[excLen][2];
            for (int i = 0; i < excLen; i++) {
                currentExcluded[i][0] = -1; // will trigger loads
            }
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

        /**
         * Main matching algorithm. In the simplest expression, the body of the
         * main loop would be:
         * <ul>
         * <li> Input: n+m iterators holding (docNum, matching position)
         * <li> 1) determine the doc which is checked: this is the smallest
         * docNum that appears in heads of iterators
         * <li> 2) Keep best matching positions for both types
         * (required/excluded)
         * <li> 3) call next() on all iterators that where about the checked
         * document
         * <li> 4) if best matching position exists for required and is smaller
         * than for excluded, set document number and return true
         * </ul>
         *
         * In practice, this is a bit more complicated by the fact occurrence
         * positions can be read just once. store some data that is not about
         * the checked document.
         *
         * @return true if there is a matching document
         * @throws IOException
         */
        @Override
        public boolean next() throws IOException {
            // best position of a required term on checkedDoc
            int bestRequired = Integer.MAX_VALUE;
            int loopPos = -1;
            boolean thisMatch = true;
            boolean allFinished = false; // if true at the end of the loop,
            // will mean that we went till the end
            // of iterators.

            while (!allFinished) {

                allFinished = true;

                // Find checked document this is about
                // and corresponding best position
                int checkedDoc = Integer.MAX_VALUE;
                int loopDoc;
                for (int i = 0; i < reqLen; i++) {
                    loopDoc = currentRequired[i][0];
                    if (loopDoc == -2) { // this iterator is exhausted
                        continue;
                    }
                    if (loopDoc > doc) {
                        // at least one piece of valid data.
                        allFinished = false;
                        loopPos = currentRequired[i][1];
                    } else {
                        // obsolete or unitialized data needs to loaded
                        if (requiredTps[i].skipTo(doc + 1)) {
                            loopDoc = requiredTps[i].doc();
                            loopPos = requiredTps[i].nextPosition();
                            // Store in case this not about the doc to check now
                            currentRequired[i][0] = loopDoc;
                            currentRequired[i][1] = loopPos;
                            // didn't hit the wall
                            allFinished = false;
                        } else {
                            currentRequired[i][0] = -2;
                            continue; // pass to next required
                        }
                    }

                    if (loopDoc < checkedDoc) {
                        checkedDoc = loopDoc;
                        bestRequired = loopPos;
                    }
                    if (loopDoc == checkedDoc && loopPos < bestRequired) {
                        bestRequired = loopPos;
                    }

                }

                if (allFinished) {
                    return false; // Further check would be done on invalid
                                    // data
                }

                // Iteration on exclusion matches.
                // Now checkedDoc is fixed. Indeed, excluding occurrences for
                // doc that come before
                // the first with allowing occurrences certainly don't yield a
                // valid match
                thisMatch = true;
                for (int i = 0; i < excLen; i++) {
                    loopDoc = currentExcluded[i][0];
                    if (loopDoc == -2) {
                        continue;
                    }
                    if (loopDoc < checkedDoc) {
                        if (excludedTps[i].skipTo(checkedDoc)) {
                            loopDoc = excludedTps[i].doc();
                            loopPos = excludedTps[i].nextPosition();
                            currentExcluded[i][0] = loopDoc;
                        } else {
                            currentExcluded[i][0] = -2;
                            continue;
                        }
                    } else {
                        loopPos = currentExcluded[i][1];
                    }

                    if (loopDoc == checkedDoc) {
                        if (loopPos < bestRequired) {
                            thisMatch = false;
                        }
                    } else { // keep for later use
                        currentExcluded[i][0] = loopDoc;
                        currentExcluded[i][1] = loopPos;
                    }
                }

                doc = checkedDoc; // for next iteration of required loop
                if (thisMatch) {
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

        /**
         * This implementation is equivalent to next() if called for current
         * doc. Note that this is in agreement with spec and example.
         *
         * @see org.apache.lucene.search.Scorer#skipTo(int)
         */
        @Override
        public boolean skipTo(int minDoc) throws IOException {
            if (minDoc > doc) {
                doc = minDoc - 1;
                return next();
            }
            if (minDoc == doc) {
                // Might not be a particular case
                return next();
            }
            return true;
        }
    }

}
