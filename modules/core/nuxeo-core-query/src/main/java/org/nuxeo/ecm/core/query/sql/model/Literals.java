/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 *
 */
package org.nuxeo.ecm.core.query.sql.model;

import static org.nuxeo.common.utils.DateUtils.toZonedDateTime;

import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.query.QueryParseException;

/**
 * Helper class for {@link Literal} and {@link LiteralList}.
 *
 * @since 9.3
 */
public class Literals {

    public static Object valueOf(Operand operand) {
        if (operand instanceof LiteralList) {
            return valueOf((LiteralList) operand);
        } else if (operand instanceof Literal) {
            return valueOf((Literal) operand);
        }
        throw new QueryParseException("Operand is not a Literal neither a LiteralList, op=" + operand);
    }

    public static List<Object> valueOf(LiteralList litList) {
        return litList.stream().map(Literals::valueOf).collect(Collectors.toList());
    }

    public static Object valueOf(Literal lit) {
        if (lit instanceof BooleanLiteral) {
            return valueOf((BooleanLiteral) lit);
        } else if (lit instanceof DateLiteral) {
            return valueOf((DateLiteral) lit);
        } else if (lit instanceof DoubleLiteral) {
            return valueOf((DoubleLiteral) lit);
        } else if (lit instanceof IntegerLiteral) {
            return valueOf((IntegerLiteral) lit);
        } else if (lit instanceof StringLiteral) {
            return valueOf((StringLiteral) lit);
        }
        throw new QueryParseException("Unknown literal: " + lit);
    }

    public static Object valueOf(BooleanLiteral lit) {
        return Boolean.valueOf(lit.value);
    }

    public static ZonedDateTime valueOf(DateLiteral lit) {
        return lit.value; // TODO onlyDate
    }

    public static Double valueOf(DoubleLiteral lit) {
        return Double.valueOf(lit.value);
    }

    public static Long valueOf(IntegerLiteral lit) {
        return Long.valueOf(lit.value);
    }

    public static String valueOf(StringLiteral lit) {
        return lit.value;
    }

    public static Literal toLiteral(Object value) {
        if (value instanceof Boolean) {
            return new BooleanLiteral(((Boolean) value).booleanValue());
        } else if (value instanceof Calendar) {
            return new DateLiteral(toZonedDateTime((Calendar) value));
        } else if (value instanceof Date) {
            return new DateLiteral(toZonedDateTime((Date) value));
        } else if (value instanceof ZonedDateTime) {
            return new DateLiteral((ZonedDateTime) value);
        } else if (value instanceof Temporal) {
            return new DateLiteral(ZonedDateTime.from((Temporal) value));
        } else if (value instanceof Double) {
            return new DoubleLiteral((Double) value);
        } else if (value instanceof Float) {
            return new DoubleLiteral((Float) value);
        } else if (value instanceof Integer) {
            return new IntegerLiteral((Integer) value);
        } else if (value instanceof Long) {
            return new IntegerLiteral((Long) value);
        } else if (value instanceof String) {
            return new StringLiteral((String) value);
        }
        throw new NuxeoException("Unknown type to convert to literal, value=" + value);
    }

}
