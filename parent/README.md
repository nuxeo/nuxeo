# Nuxeo Parent POM

Nuxeo provides a parent POM (`org.nuxeo:nuxeo-parent`) for external projects or modules depending on
the Nuxeo Server and wanting:

- to follow the latest `master` build, when using a `SNAPSHOT` version.
- to depend on a promoted release, when using a fixed version.

## Usage

To follow the latest `master` build, for instance `X.Y.Z`, set your parent POM to:

```xml
<parent>
  <groupId>org.nuxeo</groupId>
  <artifactId>nuxeo-parent</artifactId>
  <version>X.Y-SNAPSHOT</version>
</parent>
```

To depend on a promoted release, for instance `X.Y`, set your parent POM to:

```xml
<parent>
  <groupId>org.nuxeo</groupId>
  <artifactId>nuxeo-parent</artifactId>
  <version>X.Y</version>
</parent>
```

## Full POM Sample

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.nuxeo</groupId>
    <artifactId>nuxeo-parent</artifactId>
    <version>X.Y</version>
  </parent>

  <groupId>org.nuxeo.sample</groupId>
  <artifactId>nuxeo-sample</artifactId>
  <name>Nuxeo Sample Module</name>

  ...

  <repositories>
    <repository>
      <id>maven-public</id>
      <url>https://packages.nuxeo.com/repository/maven-public/</url>
      <releases>
        <enabled>true</enabled>
      </releases>
      <snapshots>
        <updatePolicy>always</updatePolicy>
        <enabled>true</enabled>
      </snapshots>
    </repository>
  </repositories>

  <pluginRepositories>
    <pluginRepository>
      <id>maven-public</id>
      <url>https://packages.nuxeo.com/repository/maven-public/</url>
      <releases>
        <enabled>true</enabled>
      </releases>
      <snapshots>
        <updatePolicy>always</updatePolicy>
        <enabled>true</enabled>
      </snapshots>
    </pluginRepository>
  </pluginRepositories>

</project>
```

## Local Clone

Note that on the `master` branch of a local clone of `nuxeo/nuxeo`, the `org.nuxeo:nuxeo-parent` POM completely follows
the current `SNAPSHOT` version (X.Y-SNAPSHOT), but not the latest `master` build.
