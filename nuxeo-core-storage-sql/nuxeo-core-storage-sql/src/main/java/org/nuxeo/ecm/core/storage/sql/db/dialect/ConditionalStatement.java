/*
 * (C) Copyright 2008-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql.db.dialect;

/**
 * Class holding info about a conditional statement whose execution may depend
 * on a preceding one to check if it's needed.
 *
 * @author Florent Guillaume
 */
public class ConditionalStatement {

    /**
     * Does this have to be executed early or late?
     */
    public final boolean early;

    /**
     * If {@code TRUE}, then always to the {@link #preStatement}, if {@code
     * FALSE} never do it, if {@code null} then use {@link #checkStatement} to
     * decide.
     */
    public final Boolean doPre;

    /**
     * If this returns something, then do the {@link #preStatement}.
     */
    public final String checkStatement;

    /**
     * Statement to execute before the actual statement.
     */
    public final String preStatement;

    /**
     * Main statement.
     */
    public final String statement;

    public ConditionalStatement(boolean early, Boolean doPre,
            String checkStatement, String preStatement, String statement) {
        this.early = early;
        this.doPre = doPre;
        this.checkStatement = checkStatement;
        this.preStatement = preStatement;
        this.statement = statement;
    }

}
