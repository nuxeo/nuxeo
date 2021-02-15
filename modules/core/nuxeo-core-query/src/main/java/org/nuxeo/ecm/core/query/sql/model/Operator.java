/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.core.query.sql.model;

import org.nuxeo.ecm.core.query.sql.parser.sym;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class Operator implements ASTNode {

    private static final long serialVersionUID = -7547286202724191250L;

    public final int id;

    protected Operator(int id) {
        this.id = id;
    }

    @Override
    public void accept(IVisitor visitor) {
        visitor.visitOperator(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Operator) {
            return id == ((Operator) obj).id;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Integer.valueOf(id).hashCode();
    }

    public static final Operator SUM = new Operator(sym.SUM) {
        private static final long serialVersionUID = 8756154412317236768L;

        @Override
        public String toString() {
            return "+";
        }
    };

    public static final Operator SUB = new Operator(sym.SUB) {
        private static final long serialVersionUID = -4503017673965713373L;

        @Override
        public String toString() {
            return "-";
        }
    };

    public static final Operator MUL = new Operator(sym.MUL) {
        private static final long serialVersionUID = 5380905007476395593L;

        @Override
        public String toString() {
            return "*";
        }
    };

    public static final Operator DIV = new Operator(sym.SUB) {
        private static final long serialVersionUID = -4386102635337691794L;

        @Override
        public String toString() {
            return "/";
        }
    };

    public static final Operator LT = new Operator(sym.LT) {
        private static final long serialVersionUID = -2033782302546604350L;

        @Override
        public String toString() {
            return "<";
        }
    };

    public static final Operator GT = new Operator(sym.GT) {
        private static final long serialVersionUID = -2450518400281430950L;

        @Override
        public String toString() {
            return ">";
        }
    };

    public static final Operator EQ = new Operator(sym.EQ) {
        private static final long serialVersionUID = -1972357682689344169L;

        @Override
        public String toString() {
            return "=";
        }
    };

    public static final Operator NOTEQ = new Operator(sym.NOTEQ) {
        private static final long serialVersionUID = -8652273867850494833L;

        @Override
        public String toString() {
            return "<>";
        }
    };

    public static final Operator LTEQ = new Operator(sym.LTEQ) {
        private static final long serialVersionUID = 4069135918287483949L;

        @Override
        public String toString() {
            return "<=";
        }
    };

    public static final Operator GTEQ = new Operator(sym.GTEQ) {
        private static final long serialVersionUID = 1626223797539530067L;

        @Override
        public String toString() {
            return ">=";
        }
    };

    public static final Operator AND = new Operator(sym.AND) {
        private static final long serialVersionUID = -6609748385590865515L;

        @Override
        public String toString() {
            return "AND";
        }
    };

    public static final Operator NOT = new Operator(sym.NOT) {
        private static final long serialVersionUID = -5748677478788963504L;

        @Override
        public String toString() {
            return "NOT";
        }
    };

    public static final Operator OR = new Operator(sym.OR) {
        private static final long serialVersionUID = -2712197732723369571L;

        @Override
        public String toString() {
            return "OR";
        }
    };

    public static final Operator LIKE = new Operator(sym.LIKE) {
        private static final long serialVersionUID = 8858605454924964544L;

        @Override
        public String toString() {
            return "LIKE";
        }
    };

    public static final Operator ILIKE = new Operator(sym.ILIKE) {
        private static final long serialVersionUID = 1L;

        @Override
        public String toString() {
            return "ILIKE";
        }
    };

    public static final Operator IN = new Operator(sym.IN) {
        private static final long serialVersionUID = 3202420273042048804L;

        @Override
        public String toString() {
            return "IN";
        }
    };

    public static final Operator BETWEEN = new Operator(sym.BETWEEN) {
        private static final long serialVersionUID = 805484346863471707L;

        @Override
        public String toString() {
            return "BETWEEN";
        }
    };

    public static final Operator NOTLIKE = new Operator(sym.NOTLIKE) {
        private static final long serialVersionUID = -7546164324575815543L;

        @Override
        public String toString() {
            return "NOT LIKE";
        }
    };

    public static final Operator NOTILIKE = new Operator(sym.NOTILIKE) {
        private static final long serialVersionUID = 1L;

        @Override
        public String toString() {
            return "NOT ILIKE";
        }
    };

    public static final Operator NOTIN = new Operator(sym.NOTIN) {
        private static final long serialVersionUID = -6960118201471749419L;

        @Override
        public String toString() {
            return "NOT IN";
        }
    };

    public static final Operator NOTBETWEEN = new Operator(sym.NOTBETWEEN) {
        private static final long serialVersionUID = -7587336224759887334L;

        @Override
        public String toString() {
            return "NOT BETWEEN";
        }
    };

    public static final Operator STARTSWITH = new Operator(sym.STARTSWITH) {
        private static final long serialVersionUID = 2438517467243495667L;

        @Override
        public String toString() {
            return "STARTSWITH";
        }
    };

    public static final Operator ISNULL = new Operator(sym.ISNULL) {
        private static final long serialVersionUID = 4907947855862157696L;

        @Override
        public String toString() {
            return "IS NULL";
        }
    };

    public static final Operator ISNOTNULL = new Operator(sym.ISNOTNULL) {
        private static final long serialVersionUID = 312081088718468562L;

        @Override
        public String toString() {
            return "IS NOT NULL";
        }
    };

}
