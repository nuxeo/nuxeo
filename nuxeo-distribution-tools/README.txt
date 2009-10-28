This module provides a maven and ant integration that can be used in 2 ways:

- as a standalone application (that is embedding a maven 2.0.9 engine and ant 1.7.0 engine) 

- as a maven plugin (that can be used from any maven project using the locally installed maven). 

Ant 1.7.0 will be used.

Also a freemarker integration is done to be able to generate files from
templates. See the freemarker section.

The application can be used to assemble (e.g. build) applications using ant
constructs by retrieving components using maven dependency graph.

So the main functionality provided by this application is an artifact graph. 

All others functionality (like custom ant tasks) are using this graph to
resolve artifact files.

The artifact Graph
-------------------------------------------------------------------------

In order to use the artifact graph you should first construct it then you can
lookup artifacts and their dependencies from the constructed graph.

A graph node is bound to an artifact and have edges that comes in (linking the
node with the one which depend on it) and goes out (linking the node with a
dependency artifact node).

To build the graph you first define the graph roots (nodes that have no
incoming edges).

Then you can expand each root node as desired (using a fixed depth or expanding
it completely).

By expanding nodes their dependencies are added to the graph and in/out edges
are created.    

If you want to include in the graph only direct dependency you can use a depth
(e.g. level) of 1.

Using a value of "all" you will expand the entire dependency sub-tree of the
node. 

When you are finishing 'expanding' (e.g. constructing) the tree you can start
using it by doing lookups on artifact node keys. These lookups can use complete
keys (including groupId, artifactId, version etc) but also wildcard keys that
may select multiple nodes from the graph. 

Returned artifacts can be then used directly in any task that accept file
resources like copy, delete, move, zip etc. 

When using an artifact in ant you are in fact using the artifact file (usually
a JAR). An artifact may have multiple attached files.

In order to use the desired file you need to specify the classifier of that
file in the artifact key.

Artifact Node keys
-------------------

An artifact is uniquely defined in the graph using the following key:

  groupId:artifactId:version:type

This means a single node can be linked to multiple artifact files if the
corresponding artifact have attached files.

You can in that case use the maven classifier of the attached file to choose
the right file you want to use in ant. 

When searching for an artifact you can specify only the first components you
want to match. So all of the following combinations are correct lookup keys:

  groupId
  groupId:artifactId
  groupId:artifactId:version
  groupId:artifactId:version:type


----------------------------------------------------------------------
Tasks
----------------------------------------------------------------------

Graph tasks
-----------------------

  <artifact:graph> -> build the graph
  <artifact:expand> -> expand artifact nodes in the current graph

The expand task is expanding one or more selected nodes from the graph.

You can use the depth attribute to control the depth of the expansion. The
special value 'all' means all sub-tree will be expanded. The default value of
depth is 1.

The expansion is done on selected artifacts given a node key (as discussed
above).  If no key is given all graph roots will be selected.

Example:

  <artifact:expand key="org.nuxeo.runtime:nuxeo-runtime" depth="all" />
  <artifact:expand /> <!-- expand all roots -->

Artifact File Resources
------------------------

Artifact file resources are used to select the file for the specified
artifacts.

You can use classifiers if you want a specific file.

There are 4 artifact file resources types:

<artifact:file>         -> selects a single artifact
<artifact:resolveFile>  -> selects a single remote artifact that is not specified by the graph. This is not using the graph but directly maven repositories.
<artifact:set>          -> selects a set of artifacts. can use includes and excludes clauses (filter are supported) 
<artifact:dependencies> -> selects the dependencies of an artifact (the depth can be controlled and filter are supported)


<artifact:file> have the following attributes:

  - groupId
  - artifactId
  - version
  - type
  - classifier
  - key

You must specify at least the 'key' attribute or one or more of the other
attributes.

If both key and other attributes are specified the 'key' take precedence.

The key format is the same as the node artifact key format described above.

Example:

  <artifact:file key="nuxeo-runtime"> will get the file of the first artifact
  found having the artifactId == "nuxeo-runtime" 

  <artifact:file key="org.nuxeo.runtime:nuxeo-runtime"> will get the file of the
  first artifact found having the groupId == "org.nxueo.runtime" and artifactId
  == "nuxeo-runtime"

  <artifact:file key="nuxeo-runtime;allinone"> - the ';' is a shortcut to be
  able to specify the classifier inside a node key. 

  <artifact:file artifactId="nuxeo-runtime" classifier="allinone"> this is
  identical to the previous example.

Note: using 'key' may generate faster lookups. (it's a prefix search on a tree
map).

Example:

    <copy todir="${maven.project.build.directory}">
    <artifact:file artifactId="nuxeo-runtime"/>
    <artifact:dependencies artifactId="nuxeo-runtime">
      <excludes>
        <artifact scope="test"/>
        <artifact scope="provided"/>
      </excludes>
    </artifact:dependencies>
    </copy>

-------------------------------------------------------------------------

Standalone application
----------------------
TODO

-------------------------------------------------------------------------

Integration as a maven plugin.
------------------------------

The following properties are exported from maven to ant build file:

  basedir -> maven.basedir
  project.name -> maven.project.name
  project.artifactId -> maven.project.artifactId
  project.groupId -> maven.project.groupId        
  project.version -> maven.project.version
  project.packaging -> maven.project.packaging
  project.id -> maven.project.id
  project.build.directory -> maven.project.build.directory
  project.build.outputDirectory -> maven.project.build.outputDirectory
  project.build.finalName -> maven.project.build.finalName

Also, any user defined maven property will be imported as an ant property. 

Also, for every active maven profile a property of the following form is
created:

  maven.profile.X = true

where X is the profile name.

This way you can use conditional ant constructs like:

  <target if="maven.profile.X">

or

  <target unless="maven.profile.X">

to conditionally execute tasks depending on whether a profile is active or not.

Maven profiles are also exported as ant profiles so you can use the custom
nx:profile tasks to conditionally execute code. Example:

<nx:profile name="X">
 ... put any ant construct here that will be executed only if profile X is active ..
</nx:profile>
  
The current maven pom (project) is put as a root into the artifact graph.

If expand > 0 (you can define this in pom.xml as a plugin configuration
property), then the project node will be expanded using a depth equals to the
expand property.

Example: if you use expand=1 -> the direct dependencies of the project are
added to the graph. 

Different mojo instances can be used in different threads, each of them will
have it's own graph. (The mojo is bound to a thread variable so that ant will
use the mojo bound to the current thread).


----------------------------------------------


Freemarker Integration
----------------------

TODO

-----------------------------------------------
