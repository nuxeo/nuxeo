/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.storage.marklogic;

import org.nuxeo.ecm.core.api.CursorResult;

import com.marklogic.xcc.ResultItem;
import com.marklogic.xcc.ResultSequence;
import com.marklogic.xcc.Session;

/**
 * {@link CursorResult} for MarkLogic which handles the session close.
 *
 * @since 9.1
 */
public class MarkLogicCursorResult extends CursorResult<ResultSequence, ResultItem> {

    protected final Session session;

    public MarkLogicCursorResult(Session session, ResultSequence cursor, int batchSize, int keepAliveSeconds) {
        super(cursor, batchSize, keepAliveSeconds);
        this.session = session;
    }

    @Override
    public boolean hasNext() {
        return cursor != null && cursor.hasNext();
    }

    @Override
    public ResultItem next() {
        return cursor.next();
    }

    @Override
    public void close() {
        // session close will close automatically the cursor (ie: result sequence)
        session.close();
        // Call super close to clear cursor
        super.close();
    }

}
