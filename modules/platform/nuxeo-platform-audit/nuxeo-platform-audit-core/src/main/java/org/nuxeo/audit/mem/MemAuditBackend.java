/*
 * (C) Copyright 2024-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.audit.mem;

import static org.nuxeo.audit.io.LogEntryJsonWriter.isJsonContent;
import static org.nuxeo.common.utils.DateUtils.toZonedDateTime;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.audit.api.LogEntryConstants;
import org.nuxeo.audit.api.LogEntryList;
import org.nuxeo.audit.service.AbstractAuditBackend;
import org.nuxeo.audit.service.AuditBackend;
import org.nuxeo.common.collections.CircularLinkedHashMap;
import org.nuxeo.common.utils.DateUtils;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.CursorResult;
import org.nuxeo.ecm.core.api.CursorService;
import org.nuxeo.ecm.core.query.sql.model.Literals;
import org.nuxeo.ecm.core.query.sql.model.MultiExpression;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.OrderByExpr;
import org.nuxeo.ecm.core.query.sql.model.OrderByList;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.core.query.sql.model.Reference;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @since 2025.0
 */
public class MemAuditBackend extends AbstractAuditBackend
        implements AuditBackend.CursorServiceScroll<Iterator<LogEntry>, LogEntry> {

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    protected final Map<Long, LogEntry> entries = Collections.synchronizedMap(new CircularLinkedHashMap<>(10_000));

    protected final CursorService<Iterator<LogEntry>, LogEntry, String> cursorService = new CursorService<>(
            entry -> String.valueOf(entry.getId()));

    @Override
    public Long getEventsCount(String eventId) {
        return entries.values().stream().map(LogEntry::getEventId).filter(Predicate.isEqual(eventId)).count();
    }

    @Override
    public void insertLogs(Collection<LogEntry> entries) {
        var conflicts = new ArrayList<String>();
        for (var entry : entries) {
            synchronized (this) {
                if (entry.getId() == 0L || entry.getLogDate() == null) {
                    throw new IllegalArgumentException("Log entry must have an id and log daCte to be inserted");
                } else if (this.entries.containsKey(entry.getId())) {
                    conflicts.add("Log entry with id: %s already exists".formatted(entry.getId()));
                } else {
                    this.entries.put(entry.getId(),
                            entry.builder().extended(mapJsonContent(entry.getExtended())).build());
                }
            }
        }
        if (!conflicts.isEmpty()) {
            var exception = new ConcurrentUpdateException("Concurrent update");
            conflicts.forEach(exception::addInfo);
            throw exception;
        }
    }

    protected Map<String, Object> mapJsonContent(Map<String, Object> extended) {
        var newExtended = new HashMap<String, Object>(extended.size());
        for (var entry : extended.entrySet()) {
            var value = entry.getValue();
            if (value instanceof String string && isJsonContent(string)) {
                try {
                    value = MAPPER.readValue(string, Map.class);
                } catch (JsonProcessingException e) {
                    // ignore invalid JSON content, same behavior than LogEntryJsonWriter
                    value = null;
                }
            }
            newExtended.put(entry.getKey(), value);
        }
        return newExtended;
    }

    @Override
    public LogEntry getLogEntryByID(long id) {
        return entries.get(id);
    }

    @Override
    public LogEntryList queryLogs(QueryBuilder builder) {
        // prepare parameters
        var queryPredicate = builder.predicate();
        var queryOrders = builder.orders();
        long queryOffset = builder.offset();
        long queryLimit = builder.limit() == 0 ? Long.MAX_VALUE : builder.limit();

        // create Predicate filter
        Predicate<LogEntry> predicate = createPredicate(queryPredicate);

        // create Comparator order
        Comparator<LogEntry> comparator = createComparator(queryOrders);

        var result = entries.values()
                            .stream()
                            .filter(predicate)
                            .sorted(comparator)
                            .skip(queryOffset)
                            .limit(queryLimit)
                            .toList();
        long totalCount = entries.values().stream().filter(predicate).count();
        return new LogEntryList(result, totalCount);
    }

    /** @since 2025.18 */
    @Override
    public CursorResult<Iterator<LogEntry>, LogEntry> scrollLogIdsAsCursor(QueryBuilder builder, int batchSize,
            Duration keepAlive) {
        // prepare parameters
        var queryPredicate = builder.predicate();
        var queryOrders = builder.orders();

        // create Predicate filter
        Predicate<LogEntry> predicate = createPredicate(queryPredicate);

        // create Comparator order
        Comparator<LogEntry> comparator = createComparator(queryOrders);

        var iterator = entries.values().stream().filter(predicate).sorted(comparator).iterator();
        return new CursorResult<>(iterator, batchSize, (int) keepAlive.toSeconds());
    }

    @SuppressWarnings("unchecked")
    protected Predicate<LogEntry> createPredicate(org.nuxeo.ecm.core.query.sql.model.Predicate queryPredicate) {
        Operator operator = queryPredicate.operator;
        if (queryPredicate instanceof MultiExpression multiExpression) {
            return multiExpression.predicates.stream()
                                             .map(this::createPredicate)
                                             .reduce(operator == Operator.OR ? Predicate::or : Predicate::and)
                                             .orElse(entry -> true);
        } else if (operator == Operator.AND) {
            return createPredicate((org.nuxeo.ecm.core.query.sql.model.Predicate) queryPredicate.lvalue).and(
                    createPredicate((org.nuxeo.ecm.core.query.sql.model.Predicate) queryPredicate.rvalue));
        } else if (operator == Operator.OR) {
            return createPredicate((org.nuxeo.ecm.core.query.sql.model.Predicate) queryPredicate.lvalue).or(
                    createPredicate((org.nuxeo.ecm.core.query.sql.model.Predicate) queryPredicate.rvalue));
        } else if (operator == Operator.NOT) {
            return Predicate.not(createPredicate((org.nuxeo.ecm.core.query.sql.model.Predicate) queryPredicate.lvalue));
        } else {
            // current implementation only use Predicate with a simple Reference for left and right
            String leftName = ((Reference) queryPredicate.lvalue).name;
            Function<LogEntry, ?> leftMapper = switch (leftName) {
                case LogEntryConstants.LOG_ID -> LogEntry::getId;
                case LogEntryConstants.LOG_CATEGORY -> LogEntry::getCategory;
                case LogEntryConstants.LOG_COMMENT -> LogEntry::getComment;
                case LogEntryConstants.LOG_EVENT_ID -> LogEntry::getEventId;
                case LogEntryConstants.LOG_EVENT_DATE -> entry -> DateUtils.toZonedDateTime(entry.getEventDate());
                case LogEntryConstants.LOG_DOC_LIFE_CYCLE -> LogEntry::getDocLifeCycle;
                case LogEntryConstants.LOG_DOC_PATH -> LogEntry::getDocPath;
                case LogEntryConstants.LOG_DOC_TYPE -> LogEntry::getDocType;
                case LogEntryConstants.LOG_DOC_UUID -> LogEntry::getDocUUID;
                case LogEntryConstants.LOG_LOG_DATE -> entry -> DateUtils.toZonedDateTime(entry.getLogDate());
                case LogEntryConstants.LOG_PRINCIPAL_NAME -> LogEntry::getPrincipalName;
                case LogEntryConstants.LOG_REPOSITORY_ID -> LogEntry::getRepositoryId;
                case String k when k.startsWith(LogEntryConstants.LOG_EXTENDED) -> {
                    Function<LogEntry, ?> mapper = LogEntry::getExtended;
                    String[] parts = k.split("/");
                    if (parts.length >= 2) {
                        for (int i = 1; i < parts.length; i++) {
                            String part = parts[i];
                            mapper = mapper.andThen(parent -> ((Map<String, ?>) parent).get(part));
                        }
                    }
                    yield mapper;
                }
                case String k -> throw new IllegalArgumentException("Unsupported reference: " + k);
            };
            if (operator == Operator.ISNULL) {
                return entry -> leftMapper.apply(entry) == null;
            } else if (operator == Operator.ISNOTNULL) {
                return entry -> leftMapper.apply(entry) != null;
            }
            try {
                Object rightValue = Literals.valueOf(queryPredicate.rvalue);
                Predicate<Object> predicate;
                if (operator == Operator.EQ) {
                    predicate = value -> Objects.equals(value, rightValue);
                } else if (operator == Operator.NOTEQ) {
                    predicate = value -> !Objects.equals(value, rightValue);
                } else if (operator == Operator.LT) {
                    predicate = value -> compare(value, rightValue) < 0;
                } else if (operator == Operator.LTEQ) {
                    predicate = value -> compare(value, rightValue) <= 0;
                } else if (operator == Operator.GTEQ) {
                    predicate = value -> compare(value, rightValue) >= 0;
                } else if (operator == Operator.GT) {
                    predicate = value -> compare(value, rightValue) > 0;
                } else if (operator == Operator.IN) {
                    predicate = value -> ((List<?>) rightValue).contains(value);
                } else if (operator == Operator.NOTIN) {
                    predicate = value -> !((List<?>) rightValue).contains(value);
                } else if (operator == Operator.BETWEEN) {
                    // compare everything on UTC time zone
                    var lowerBound = ((List<ZonedDateTime>) rightValue).getFirst().withZoneSameInstant(ZoneOffset.UTC);
                    var upperBound = ((List<ZonedDateTime>) rightValue).getLast().withZoneSameInstant(ZoneOffset.UTC);
                    predicate = value -> lowerBound.compareTo((ZonedDateTime) value) <= 0
                            && upperBound.compareTo((ZonedDateTime) value) >= 0;
                } else if (operator == Operator.NOTBETWEEN) {
                    // compare everything on UTC time zone
                    var lowerBound = ((List<ZonedDateTime>) rightValue).getFirst().withZoneSameInstant(ZoneOffset.UTC);
                    var upperBound = ((List<ZonedDateTime>) rightValue).getLast().withZoneSameInstant(ZoneOffset.UTC);
                    predicate = value -> lowerBound.compareTo((ZonedDateTime) value) > 0
                            || upperBound.compareTo((ZonedDateTime) value) < 0;
                } else if (operator == Operator.STARTSWITH) {
                    predicate = value -> value != null && ((String) value).startsWith(String.valueOf(rightValue));
                } else {
                    throw new IllegalArgumentException("Unsupported operator: " + operator + " on field: " + leftName);
                }
                return entry -> predicate.test(leftMapper.andThen(this::convertToLiteralsJavaType).apply(entry));
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("Unsupported operator: " + operator + " on field: " + leftName, e);
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected int compare(Object leftValue, Object rightValue) {
        return Comparator.nullsLast((l, r) -> ((Comparable) l).compareTo(r)).compare(leftValue, rightValue);
    }

    protected Object convertToLiteralsJavaType(Object leftValue) {
        if (leftValue instanceof Integer integerValue) {
            leftValue = integerValue.longValue();
        } else if (leftValue instanceof Float floatValue) {
            leftValue = floatValue.doubleValue();
        } else if (leftValue instanceof Calendar calendarValue) {
            leftValue = toZonedDateTime(calendarValue);
        } else if (leftValue instanceof Date dateValue) {
            leftValue = toZonedDateTime(dateValue);
        } else if (leftValue instanceof Temporal temporalValue) {
            leftValue = ZonedDateTime.from(temporalValue);
        }
        return leftValue;
    }

    protected Comparator<LogEntry> createComparator(OrderByList queryOrders) {
        return queryOrders.stream()
                          .map(this::createComparator)
                          .reduce(Comparator::thenComparing)
                          .orElse(Comparator.comparingLong(LogEntry::getId));
    }

    protected Comparator<LogEntry> createComparator(OrderByExpr queryOrder) {
        var comparator = switch (queryOrder.reference.name) {
            case LogEntryConstants.LOG_ID -> Comparator.comparingLong(LogEntry::getId);
            case LogEntryConstants.LOG_CATEGORY -> Comparator.comparing(LogEntry::getCategory);
            case LogEntryConstants.LOG_COMMENT -> Comparator.comparing(LogEntry::getComment);
            case LogEntryConstants.LOG_EVENT_ID -> Comparator.comparing(LogEntry::getEventId);
            case LogEntryConstants.LOG_EVENT_DATE -> Comparator.comparing(LogEntry::getEventDate);
            case LogEntryConstants.LOG_DOC_LIFE_CYCLE -> Comparator.comparing(LogEntry::getDocLifeCycle);
            case LogEntryConstants.LOG_DOC_PATH -> Comparator.comparing(LogEntry::getDocPath);
            case LogEntryConstants.LOG_DOC_TYPE -> Comparator.comparing(LogEntry::getDocType);
            case LogEntryConstants.LOG_DOC_UUID -> Comparator.comparing(LogEntry::getDocUUID);
            case LogEntryConstants.LOG_LOG_DATE -> Comparator.comparing(LogEntry::getLogDate);
            case LogEntryConstants.LOG_PRINCIPAL_NAME -> Comparator.comparing(LogEntry::getPrincipalName);
            case LogEntryConstants.LOG_REPOSITORY_ID -> Comparator.comparing(LogEntry::getRepositoryId);
            case String k -> throw new IllegalArgumentException("Unsupported reference: " + k);
        };
        if (queryOrder.isDescending) {
            return comparator.reversed();
        }
        return comparator;
    }

    @Override
    public boolean hasCapability(Capability capability) {
        return switch (capability) {
            case EXTENDED_INFO_SEARCH -> true;
            case STARTS_WITH_PARTIAL_MATCH -> true;
        };
    }

    @Override
    public CursorService<Iterator<LogEntry>, LogEntry, String> getCursorService() {
        return cursorService;
    }

    @Override
    public List<?> nativeQuery(String query, Map<String, Object> params, int pageNb, int pageSize) {
        throw new UnsupportedOperationException("Not available for in-memory implementation");
    }

    @Override
    protected void clearEntries() {
        entries.clear();
    }
}
