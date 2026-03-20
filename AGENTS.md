# Nuxeo LTS 2025 - Agent Development Guide

## Project Overview

Nuxeo is an Enterprise Content Management (ECM) platform. This is the `nuxeo-lts` monorepo (version `2025.x-SNAPSHOT`), a massive Maven multi-module Java project hosted at `github.com/nuxeo/nuxeo-lts` on branch `2025`.

### Module Hierarchy

```
nuxeo-ecm (root)
├── boms/                    # Bill of Materials (OpenSearch 1.x BOM)
├── modules/
│   ├── runtime/             # ~27 modules - Core runtime, OSGi, streams, Kafka, MongoDB driver, KV store
│   ├── core/                # ~29 modules - Document model, storage engines (SQL, MongoDB, DBS, mem), bulk ops, binary managers
│   └── platform/            # ~96 modules - High-level services: REST API, audit, auth, automation, workflows, Drive, CMIS
├── parent/                  # Re-exportable parent POM for out-of-tree builds (addons, customer projects)
├── server/                  # Server distribution (Tomcat-based launcher, NXR assembly)
├── packages/                # ~43 Nuxeo marketplace packages
├── ftests/                  # ~35 functional test modules (tiered: Tier5, Tier6, Tier7)
├── docker/                  # Docker images (nuxeo, nuxeo-benchmark)
└── ci/                      # Jenkins pipelines, Helm charts, CI scripts
```

### POM Hierarchy

```
nuxeo-ecm (root, org.nuxeo)             # All dependency versions managed here
├── nuxeo-parent (org.nuxeo)             # Re-exportable parent for external projects
└── nuxeo-modules (org.nuxeo)
      ├── nuxeo-runtime-parent           # Parent for all runtime modules
      ├── nuxeo-core-parent              # Parent for all core modules
      └── nuxeo-platform-parent          # Parent for all platform modules
            └── nuxeo-*-parent           # Some platform areas have sub-parents (e.g., audit, webengine)
```

Module POMs declare dependencies **without `<version>` tags** - all versions are centrally managed in the root POM.

### Build System

- **Maven** multi-module with root POM managing all dependency versions (~5400 lines)
- **Java 21** (Azul Zulu JDK) with `<release>21</release>`
- **Maven 3.6.3+** required
- Default build: `mvn install` (builds `boms` + `modules` only)
- Full build: `mvn install -Pdistrib,docker -DskipTests -T6`
- Code formatting: `mvn spotless:check` / `mvn spotless:apply` (Eclipse formatter, ratcheted from `origin/2025`)
- MAVEN_OPTS: `-Xmx4g -Xms2g`

## Languages

- **Java** for the main Nuxeo application code (99%+ of the codebase)
- **XML** for component declarations, extension contributions, schemas (XSD), and Maven POMs
- **FreeMarker** (`.ftl`) for server-side templates: email notifications and WebEngine page rendering
- **JavaScript** legacy only: vendored jQuery/TinyMCE in WebEngine pages (no modern frontend framework, no npm/node)
- **Groovy** for Jenkins CI pipelines and server admin scripts
- **Python** for release tooling and test infrastructure (not part of the application)
- **Shell** for Docker entrypoints and server management scripts

## Java Conventions

### Modern Java Features (USE THEM)

- **`var`**: Use local variable type inference wherever the type is obvious from context
- **Records**: Use for immutable value types and DTOs
- **Sealed interfaces/classes**: Use when appropriate for closed type hierarchies
- **Pattern matching `instanceof`**: Use `if (value instanceof String s)` instead of cast-after-check
- **Switch expressions**: Use `var x = switch (y) { case ... -> ...; };` for exhaustive switches
- **Text blocks**: Use `"""` for multi-line strings
- **`String.formatted()`**: Prefer `"text %s".formatted(value)` over `String.format()`
- **Streams**: Use `.stream()`, `.map()`, `.filter()`, `.toList()` for collection transformations
- **`List.copyOf()`, `Map.copyOf()`**: Use in record constructors for defensive copying
- **Try-with-resources**: Always use for `AutoCloseable` resources

### Code Style

- **4-space indentation** (no tabs)
- **K&R braces** (opening brace on same line)
- **~120 character line width**
- **Formatting**: Enforced by Spotless plugin with Eclipse formatter. Run `mvn spotless:apply` to auto-format
- **No wildcard imports** (enforced by Spotless)
- **Null annotations**: Use `jakarta.annotation.Nonnull` (and `jakarta.annotation.Nullable`) for null-safety contracts in method signatures. Do NOT use `jakarta.validation.constraints.NotNull` for this purpose -- that annotation is reserved for Bean Validation (runtime constraint checking), not API contracts
- **`@SuppressWarnings` conventions**:
  - `"unchecked"` for generic type casts (most common)
  - `"resource"` for `AutoCloseable` resources managed elsewhere (always add an inline comment explaining why)
  - `"deprecation"` / `"removal"` for intentional calls to deprecated APIs (e.g., backward-compatible bridges)

### Package Naming

```
org.nuxeo.runtime.*          # Runtime-layer modules
org.nuxeo.ecm.core.*         # Core-layer modules (document model, storage, events)
org.nuxeo.ecm.platform.*     # Platform-layer modules (high-level services)
org.nuxeo.common.*            # Common utilities (xmap, Environment, etc.)
org.nuxeo.lib.stream.*        # Stream/computation library
```

### Logging

Use **Log4j2** exclusively:

```java
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

private static final Logger log = LogManager.getLogger(MyClass.class);
```

- Use parameterized messages: `log.debug("Processing doc: {}", docId)`
- Use lambda suppliers for expensive computations: `log.debug("Status: {}", () -> computeStatus())`
- Never use `System.out.println` or SLF4J directly

### Javadoc

- **`@since`** tag is mandatory on all new public API (classes, methods, fields):
  - Current format: `@since 2025.XX` (CalVer: LTS year + sprint number)
  - Example: `@since 2025.17`
- Inline `// @since 2025.XX` comments are acceptable on fields and private/protected members
- Class-level Javadoc: brief one-liner descriptions
- `@author` tags: no longer used in new code (contributors listed in file header)

### License Header

Every Java file must have the Apache 2.0 license header. Use the current year:

```java
/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Your Name
 */
```

### Import Ordering

1. Static imports
2. `java.*`
3. `jakarta.*`
4. `org.*` (`org.apache.*`, `org.nuxeo.*`, etc.)
5. `com.*` (`com.google.*`, `com.fasterxml.*`, etc.)

Note: The project uses **Jakarta EE** namespace (`jakarta.inject`, `jakarta.servlet`, `jakarta.ws.rs`, `jakarta.transaction`), NOT `javax.*`.

## Backward Compatibility

- **Never break backward compatibility** unless explicitly asked to
- When adding new methods to replace old ones:
  1. Add `@Deprecated(since = "2025.XX", forRemoval = true)` to the old method
  2. Add `/** @deprecated since 2025.XX, use {@link NewMethod} instead */` Javadoc
  3. Update all internal callers to use the new method (the codebase must not use deprecated methods)

### Deprecation Format

```java
/** @deprecated since 2025.17, use {@link #newMethod()} instead */
@Deprecated(since = "2025.17", forRemoval = true)
public void oldMethod() {
    newMethod();
}
```

## Architecture & Patterns

### Component Model (Custom OSGi-inspired DI)

Nuxeo uses a **custom runtime component model** (NOT Spring, NOT CDI). Key concepts:

1. **Components** extend `DefaultComponent` and are declared in XML under `OSGI-INF/`:

```xml
<?xml version="1.0"?>
<component name="org.nuxeo.ecm.core.bulk">
  <implementation class="org.nuxeo.ecm.core.bulk.BulkComponent" />
  <service>
    <provide interface="org.nuxeo.ecm.core.bulk.BulkService" />
  </service>
  <extension-point name="actions">
    <object class="org.nuxeo.ecm.core.bulk.BulkActionDescriptor" />
  </extension-point>
</component>
```

2. **Service lookup** via static locator:

```java
var service = Framework.getService(BulkService.class);
```

3. **Extension points** accept **Descriptors** (XML-to-Java mapping via XMap annotations).
   Fields use `protected` visibility (the established convention). Setters are not used -- XMap sets fields via reflection, and `merge()` accesses fields directly:

```java
@XObject("action")
public class BulkActionDescriptor implements Descriptor {
    @XNode("@name")
    protected String name;

    @XNode("@enabled")
    protected Boolean enabled;

    @Override
    public String getId() {
        return name;
    }

    @Override
    public BulkActionDescriptor merge(Descriptor o) {
        var other = (BulkActionDescriptor) o;
        var merged = new BulkActionDescriptor();
        merged.name = name;
        merged.enabled = getIfNull(other.enabled, enabled);
        return merged;
    }
}
```

4. **MANIFEST.MF** registers components:

```
Nuxeo-Component: OSGI-INF/bulk-component.xml,
 OSGI-INF/bulk-actions-contrib.xml
```

### Component Lifecycle

`activate()` -> `start()` -> `stop()` -> `deactivate()`

Override these in your component class when needed. Use `start()` for initialization logic.

### Descriptor Merge Utilities

When implementing `Descriptor.merge()`, use these Apache Commons utilities for field merging:

```java
import static org.apache.commons.lang3.ObjectUtils.getIfNull;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.collections4.ListUtils.union;

@Override
public MyDescriptor merge(Descriptor o) {
    var other = (MyDescriptor) o;
    var merged = new MyDescriptor();
    merged.name = getIfNull(other.name, name);                   // identity field - prefer other if non-null
    merged.label = defaultIfBlank(other.label, label);           // String: take other if non-blank
    merged.enabled = getIfNull(other.enabled, enabled);          // Object/Boolean: take other if non-null
    merged.items = union(items, other.items);                    // Lists (primitives): concatenation
    return merged;
}
```

For lists of nested `Descriptor` objects, use a map-based merge on `getId()` to recursively merge matching entries:

```java
@SuppressWarnings("unchecked")
protected <D extends Descriptor> List<D> merge(List<D> first, List<D> second) {
    var map = new HashMap<String, D>();
    first.forEach(d -> map.put(d.getId(), d));
    second.forEach(d -> map.merge(d.getId(), d, (prev, cur) -> (D) prev.merge(cur)));
    return new ArrayList<>(map.values());
}
```

### Extension Contributions (XML)

```xml
<extension target="org.nuxeo.ecm.core.bulk" point="actions">
  <action name="myAction" enabled="true">
    <!-- action configuration -->
  </action>
</extension>
```

Use `<require>` when contribution ordering matters:

```xml
<component name="org.nuxeo.my.contrib">
  <require>org.nuxeo.ecm.core.bulk.config</require>
  <extension target="org.nuxeo.ecm.core.bulk" point="actions">
    <!-- contributions here -->
  </extension>
</component>
```

### Configuration Access

Two **independent** property systems exist. They do NOT share data -- a property set in one is invisible to the other.

**1. `Framework.getProperty` -- nuxeo.conf / system properties**

For infrastructure and environment configuration (URLs, ports, file paths, JVM-level settings). Properties come from `nuxeo.conf` or `-D` JVM flags. Returns `String` only. Available immediately at startup.

```java
// Read a nuxeo.conf property
String url = Framework.getProperty("nuxeo.url", "http://localhost:8080/nuxeo");
```

**2. `ConfigurationService` -- XML-contributed application properties**

For application-level feature flags and tuning. Properties are contributed via XML extension points. Provides typed access (`String`, `int`, `boolean`, `Duration`, etc.). Available after component registration.

```java
// Read an XML-contributed property
var config = Framework.getService(ConfigurationService.class);
config.getString("my.key");
config.isBooleanTrue("my.key");
config.getInteger("my.key", 42);
config.getDuration("my.key");
```

Properties are contributed via XML:

```xml
<extension target="org.nuxeo.runtime.ConfigurationService" point="configuration">
  <property name="nuxeo.my.property">value</property>
</extension>
```

**Do NOT mix them up**: `Framework.getProperty` cannot see XML-contributed properties, and `ConfigurationService` cannot see `nuxeo.conf` properties.

**Variable substitution**: XML contributions can reference `nuxeo.conf` properties using `${property.name:=defaultValue}` syntax. These are resolved at XML load time, before the Descriptor is created. This allows XML-contributed configuration to have defaults overridable via `nuxeo.conf`:

```xml
<property name="nuxeo.bulk.scroller.keepAlive">${nuxeo.bulk.scroller.keepAlive:=300}</property>
```

### Persistence Layer

- **No JPA/Hibernate for document storage** - Nuxeo uses custom storage engines:
  - VCS (SQL): Direct JDBC in `nuxeo-core-storage-sql`
  - DBS (NoSQL): MongoDB via `nuxeo-core-storage-mongodb`
  - DBS Memory: `nuxeo-core-storage-mem` (testing)
- Key-Value store abstraction: `nuxeo-runtime-kv`

### Document Model API (CoreSession)

`CoreSession` is the central API for all document CRUD. In tests, inject it directly with `@Inject protected CoreSession session;`. In production code, obtain it from a `DocumentModel` via `doc.getCoreSession()` or receive it as a method parameter.

```java
// Create
var doc = session.createDocumentModel("/path", "my-doc", "File");
doc.setPropertyValue("dc:title", "My Document");
doc = session.createDocument(doc);

// Read
var doc = session.getDocument(new IdRef(uuid));   // by UUID
var doc = session.getDocument(new PathRef("/path/my-doc")); // by path

// Update
doc.setPropertyValue("dc:description", "Updated");
session.saveDocument(doc);

// Delete
session.removeDocument(doc.getRef());
```

- **Property access**: Use xpath-based API: `doc.getPropertyValue("dc:title")`, `doc.setPropertyValue("dc:title", value)`
- **Blobs**: `doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob(file))`
- **Adapters**: `doc.getAdapter(BlobHolder.class)` for typed access patterns
- **Privileged operations**: `CoreInstance.doPrivileged(session, s -> { ... })` for system-level access
- Key classes: `CoreSession`, `DocumentModel`, `DocumentRef`, `IdRef`, `PathRef`

### Schemas and Document Types

Document types are defined via XML contributions:

```xml
<!-- Schema registration (points to XSD file) -->
<extension target="org.nuxeo.ecm.core.schema.TypeService" point="schema">
  <schema name="dublincore" prefix="dc" src="schema/dublincore.xsd" />
</extension>

<!-- Document type definition -->
<extension target="org.nuxeo.ecm.core.schema.TypeService" point="doctype">
  <doctype name="MyDoc" extends="Document">
    <schema name="dublincore" />
    <schema name="common" />
    <schema name="file" />
    <facet name="Versionable" />
    <facet name="Commentable" />
    <subtypes>
      <type>MyChildDoc</type>
    </subtypes>
  </doctype>
</extension>
```

- **Standard schemas**: `dublincore` (prefix `dc`: title, description, creator, created, modified), `file` (content blob), `files` (attachments)
- **Facets**: Mixins adding schemas or capabilities (e.g., `Folderish`, `Versionable`, `HiddenInNavigation`)
- **SchemaManager**: `Framework.getService(SchemaManager.class)` for runtime type/schema introspection

### NXQL Queries and PageProviders

**NXQL** is the SQL-like query language for documents:

```java
var docs = session.query("SELECT * FROM Document WHERE dc:title = 'foo'");
```

- System properties use `ecm:` prefix: `ecm:uuid`, `ecm:path`, `ecm:parentId`, `ecm:primaryType`, `ecm:mixinType`, `ecm:isVersion`, `ecm:isTrashed`
- Common filter pattern: `ecm:isVersion = 0 AND ecm:mixinType != 'HiddenInNavigation' AND ecm:isTrashed = 0`

**PageProviders** are the higher-level abstraction for paginated queries, defined via XML:

```xml
<extension target="org.nuxeo.ecm.platform.query.api.PageProviderService" point="providers">
  <coreQueryPageProvider name="MY_PROVIDER">
    <pattern>SELECT * FROM Document WHERE dc:title LIKE ?</pattern>
    <pageSize>20</pageSize>
  </coreQueryPageProvider>
</extension>
```

- Obtained at runtime via `Framework.getService(PageProviderService.class).getPageProvider(name, ...)`
- Key classes: `PageProviderService`, `CoreQueryDocumentPageProvider`

### Security (ACL / ACP)

Permissions are managed via `ACP` > `ACL` > `ACE` hierarchy:

```java
var acp = doc.getACP();
var ace = ACE.builder("john", SecurityConstants.READ_WRITE).grant(true).build();
acp.addACE(ACL.LOCAL_ACL, ace);
session.setACP(doc.getRef(), acp, false); // false = merge, true = overwrite
```

- Standard permissions in `SecurityConstants`: `READ`, `WRITE`, `READ_WRITE`, `EVERYTHING`
- `ACE.BLOCK` blocks all permissions for everyone below it in the ACL
- System-level access: `CoreInstance.doPrivileged(session, s -> { ... })`

### REST API (WebEngine + JAX-RS)

- Built on **Nuxeo WebEngine** (custom framework on top of Jersey/JAX-RS)
- Uses Jakarta WS-RS annotations (`@GET`, `@POST`, `@Path`, `@Produces`)
- Resources annotated with `@WebObject(type = "...")` and extend `DefaultObject`
- Adapters annotated with `@WebAdapter(name = "...", type = "...")`
- API root: `/api/v1/` in `APIRoot` class
- Sub-resources created via `newObject(SomeClass.class, args...)` pattern

### JSON Marshalling (Custom Framework)

- **NOT raw Jackson** - Nuxeo has its own marshaller framework (`MarshallerRegistry`)
- Writers extend `AbstractJsonWriter<T>` or `ExtensibleEntityJsonWriter<T>` (enricher support)
- Readers extend `AbstractJsonReader<T>` or `EntityJsonReader<T>`
- All JSON entities have an `"entity-type"` discriminator field
- **Enrichers**: Extend `AbstractJsonEnricher<T>`, write into `"contextParameters"` JSON section, activated via request header `enrichers-document=name1,name2`

### Nuxeo Stream / Computation Framework

Data processing pipelines built on a **Directed Acyclic Graph (DAG)** of computations and streams:

- **Streams**: Named, partitioned logs (backed by Kafka or in-memory). Data flows as `Record` objects
- **Computations**: Processing units consuming from input streams and producing to output streams
- **Topology**: Built via `Topology.builder()` in a `StreamProcessorTopology` implementation

Key base classes (`org.nuxeo.lib.stream.computation`):

| Class | Purpose |
|-------|---------|
| `AbstractComputation` | Base class. Constructor: `(name, nbInputStreams, nbOutputStreams)`. Override `processRecord()` and/or `processTimer()` |
| `AbstractBatchComputation` | Accumulates records into batches, calls `batchProcess()` |
| `AbstractBulkComputation` | Bulk actions: decodes `BulkBucket`, opens `CoreSession`, calls `compute(session, ids, properties)` |

Registration via XML:

```xml
<extension target="org.nuxeo.runtime.stream.service" point="streamProcessor">
  <streamProcessor name="setProperties"
                   class="org.nuxeo.ecm.core.bulk.action.SetPropertiesAction"
                   defaultConcurrency="${nuxeo.bulk.action.setProperties.defaultConcurrency:=2}"
                   defaultPartitions="${nuxeo.bulk.action.setProperties.defaultPartitions:=4}">
    <policy name="default" maxRetries="3" delay="500ms" maxDelay="10s"
            continueOnFailure="false" />
  </streamProcessor>
</extension>
```

Topology building pattern:

```java
public class SetPropertiesAction implements StreamProcessorTopology {

    public static final String ACTION_NAME = "setProperties";
    public static final String ACTION_FULL_NAME = "bulk/" + ACTION_NAME;

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(SetPropertyComputation::new,
                               List.of(INPUT_1 + ":" + ACTION_FULL_NAME,
                                       OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

    public static class SetPropertyComputation extends AbstractBulkComputation {

        public SetPropertyComputation() {
            super(ACTION_FULL_NAME);
        }

        @Override
        protected void compute(CoreSession session, List<String> ids,
                Map<String, Serializable> properties) {
            // process document IDs
        }
    }
}
```

### Event System

- **Core events**: `EventService` with synchronous `EventListener` and async `PostCommitEventListener`
- Listeners registered via XML extension points
- Events: `documentCreated`, `documentModified`, `beforeDocumentModification`, etc.
- Domain events via Nuxeo Stream/Kafka

### Async Work: WorkManager vs Stream/Bulk

- **`BulkService`** + Stream/Computation: The **preferred approach** for async processing. Used for batch operations on document sets identified by NXQL queries, but also for single-document async patterns via Bulk Actions
- **`WorkManager`** + `AbstractWork`: Legacy mechanism for individual async tasks (media conversion, thumbnail generation). Existing code still uses it, but **do not create new `AbstractWork` implementations** -- prefer Bulk Actions instead

### Automation Framework

- Operations annotated with `@Operation(id = "...", ...)` with `@OperationMethod` typed input/output
- Exposed via REST at `/api/v1/automation`

```java
@Operation(id = "Document.AddPermission", category = Constants.CAT_DOCUMENT)
public class AddPermission {

    @Context
    protected CoreSession session;

    @Param(name = "permission")
    protected String permission;

    @Param(name = "username", required = false)
    protected String user;

    @OperationMethod
    public DocumentModel run(DocumentModel doc) {
        // operation logic
        return session.getDocument(doc.getRef());
    }
}
```

### Exception Handling

- **`NuxeoException`** extends `RuntimeException` - the primary exception class
  - Carries HTTP `statusCode` field (default 500)
  - `addInfo(String)` for context
- **`RuntimeServiceException`** for runtime-layer exceptions
- Wrap checked exceptions: `throw new NuxeoException("context message", e);`
- Never let checked exceptions leak from public APIs
- `WebEngineExceptionMapper` handles all REST exception-to-HTTP mapping

## Testing

### Framework: JUnit 4 with Custom Runner

```java
@RunWith(FeaturesRunner.class)
@Features(CoreBulkFeature.class)
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib.xml")
public class TestBulkService {

    @Inject
    protected BulkService bulkService;

    @Test
    public void testSubmitCommand() {
        // test code using injected services
    }
}
```

### Key Testing Patterns

- **`@RunWith(FeaturesRunner.class)`**: Mandatory - uses Nuxeo's custom JUnit 4 runner
- **`@Features(...)`**: Compose test capabilities (e.g., `CoreFeature`, `TransactionalFeature`, `AutomationFeature`)
- **`@Deploy("bundle.name")`**: Deploy Nuxeo bundles/components for the test
- **`@Deploy("bundle:path/to/contrib.xml")`**: Deploy specific XML contributions
- **`@Inject`**: Jakarta inject for services (NOT Spring `@Autowired`)
- **`@WithFrameworkProperty`**: Set framework properties for a test
- **`@LoggerLevel`**: Control log levels during tests
- **`@RepositoryConfig`**: Configure test repository
- **`@ConditionalIgnore`**: Skip tests conditionally (e.g., `IgnoreIfNotPostgreSQL`)
- **`TransactionalFeature.nextTransaction()`**: Commit current, wait for async tasks, and start new transaction in tests

### Test Naming

- Test classes: `Test` prefix (e.g., `TestBulkService`, `TestAWSConfigurationService`)
- Test methods: `testXxx()` pattern (e.g., `testSubmitCommand`)
- Abstract test base classes: `AbstractXxxTest` pattern

### Assertions

- Primary: `org.junit.Assert.*` (`assertEquals`, `assertNotNull`, `assertTrue`)
- Mockito: Available (`org.mockito.*`) but used selectively - prefer real Nuxeo runtime integration tests
- AssertJ: Available but rarely used

## Third-Party Libraries

### Prefer Reusing Existing Dependencies

Always check if a needed utility already exists in these libraries before adding new dependencies:

| Library | Usage | Key Classes |
|---------|-------|-------------|
| **Apache Commons Lang3** | String/Object utilities | `StringUtils`, `ObjectUtils.getIfNull()`, `BooleanUtils` |
| **Apache Commons IO** | I/O utilities | `IOUtils`, `FileUtils`, `FilenameUtils` |
| **Apache Commons Collections4** | Collection utilities | `CollectionUtils`, `MapUtils` |
| **Google Guava** | Caching, collections | `Cache`/`CacheBuilder`, `ImmutableMap`, `Lists` |
| **Jackson 2** | JSON processing | `ObjectMapper`, `JsonNode`, `JsonGenerator` (via Nuxeo marshaller framework) |
| **Apache POI** | Office documents | Used in conversion/preview |
| **PDFBox** | PDF processing | Used in conversion/preview |

### Dependencies Already Managed (Do NOT add version numbers)

All dependency versions are managed in the root `pom.xml`. Module-level `pom.xml` files declare dependencies **without** `<version>` tags. Never add version numbers to module POMs.

### Banned Dependencies

The enforcer plugin bans:
- `javax.*` artifacts (use `jakarta.*` equivalents)
- Old Jersey 1.x (`com.sun.jersey`)
- Log4j 1.x / Logback directly (use Log4j2)
- Old ASM versions

## Git Conventions

### Commit Messages

```
NXP-XXXXX: Short description of the change
```

- Always prefix with the Jira ticket ID (`NXP-` for product, `NXBT-` for build/tooling)
- Description starts with lowercase or uppercase, uses imperative or descriptive tense
- Release commits: `Release 2025.XX, update 2025.XX-SNAPSHOT to 2025.YY-SNAPSHOT`

### Branch Naming

```
{feat,fix,impr,task}-NXP-XXXXX-short-description
```

- `feat-`: Features
- `fix-`: Bug fixes
- `impr-`: Improvements/enhancements
- `task-`: Tasks/chores
- Append `-2023` or `-2025` suffix when targeting a specific LTS branch

### Release Branches

Year-based: `2023`, `2025`

## Creating New Modules

When creating a new module, follow this structure:

```
my-module/
├── pom.xml                           # Parent: appropriate nuxeo-*-parent
├── src/main/java/                    # Source code
├── src/main/resources/
│   ├── META-INF/MANIFEST.MF          # Bundle manifest with Nuxeo-Component header
│   └── OSGI-INF/
│       ├── my-service.xml            # Component declaration
│       └── my-contrib.xml            # Default contributions
├── src/test/java/                    # Tests
└── src/test/resources/
    ├── META-INF/MANIFEST.MF          # Test bundle manifest
    └── OSGI-INF/
        └── test-contrib.xml          # Test-specific contributions
```

### MANIFEST.MF Template

**Important**: MANIFEST.MF files must always end with a trailing newline. The JAR specification requires it -- the last header will be silently ignored if the file does not end with a newline.

```
Manifest-Version: 1.0
Bundle-ManifestVersion: 1
Bundle-Name: My Module Name
Bundle-SymbolicName: org.nuxeo.my.module;singleton:=true
Bundle-Vendor: Nuxeo
Nuxeo-Component: OSGI-INF/my-service.xml,
 OSGI-INF/my-contrib.xml
```

## Common Pitfalls

1. **Do NOT use Spring annotations** (`@Autowired`, `@Component`, `@Service`) - this is not a Spring project
2. **Do NOT use `javax.*` imports** - the project has migrated to `jakarta.*`
3. **Do NOT add `<version>` in module POMs** - versions are managed centrally in root POM
4. **Do NOT use SLF4J directly** - use Log4j2 API (`LogManager.getLogger`)
5. **Do NOT use JPA for document storage** - use Nuxeo's `CoreSession` / `DocumentModel` API
6. **Do NOT create REST resources with plain JAX-RS** - use WebEngine `@WebObject` / `@WebAdapter`
7. **Do NOT serialize objects with raw Jackson** - use Nuxeo's `MarshallerRegistry` framework
8. **Do NOT use JUnit 5** - the test framework is JUnit 4 with `FeaturesRunner`
