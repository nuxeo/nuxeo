

Nodes (artifacts) cannot be lazy resolved because the remote pom may contain a relocation directive like:

<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>ehcache</groupId>
  <artifactId>ehcache</artifactId>
  <version>1.2</version>

  <distributionManagement>
    <relocation>
      <groupId>net.sf.ehcache</groupId>
    </relocation>
  </distributionManagement>

</project>

This means every time you call resolveArtifact -> the artifact will be resolved immediately before being registered as a graph node.
Resolving a node means:

1. resolve the node pom.
2. relocate the artifact if needed and repeat step 1. if relocation was done.
3. if not a pom node resolve the node artifact.

Resolving dependencies is done if expand attribute is specified. To resolve dependencies the following steps are done:

1. get dependencies 
  - if a pom artifact then get dependency management deps.
  - otherwise get pom deps.
 2. resolve each dependency node and recurse if needed
  
   