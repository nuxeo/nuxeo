About this project
==================

This is a shell (command-line tool for you Windows people) that let you browse
and manage content on a CMIS repository (a bit like Cadaver for WebDAV, if you
know it).

Dependencies
------------

The project is not dependent on the Nuxeo EP code (so it could be pushed into
the Apache Chemistry codebase if wanted).

Here are the direct dependencies:
 
jline
chemistry-api
chemistry-commons
chemistry-atompub
chemistry-atompub-client

The packaging is an all in one jar containing all dependencies required by
chemistry-atompub-client (like httpclient, apache-commons, apache-logging, stax
(wstx) libs etc).

Usage
-----

After building (with "mvn install"), it you can launch it using the run.sh
command (by giving the URL where to connect), e.g.:

> ./run.sh -u Administrator -p Administrator \
  http://localhost:8080/nuxeo/site/cmis/repository

Some of the registered commands are not yet implemented. Commands use
annotations and optional *.help files to provide help content (the *.help files
must have the name of the command and be put in the same package. Ex: Ls.help) 

Available commands for now are:

help
id    - display info about the current object
mkdir
mkfile
setp
getp
setStream
getStream
pwd
ls
cd
pushd
popd
tree  - show repository tree
props - show object properties
lpwd  - local pwd (file based)
lcd   - local cd
ll    - list local directory content
lpushd
lpopd
cmds
exit

Ls listing supports colors based on object types - I've made a default schema
for demonstration (you can change this in META-INF/color.properties).

The shell has 3 modes:

- single command execution (using -e flag)
- batch execution of commands in a file (using -b)
- interactive execution (the default one)

Note that some commands are not available in all contexts (when not yet in a
repository, calling "tree" will display nothing).

I will improve this later to show only available commands depending on the
context.

One important note is that the initial context is not a repository but the APP
service. So if you do a "ls" in the initial context you will have the list of
repositories. To enter a repository, do a "cd repo_name". After entering a
repository you are in a chemistry object context so all commands should be
available. 

How to extend it
----------------

To add new commands, simply extend ChemistryCommand class and look how the
other commands works.

The most important interfaces you need to know about are Application and
Context which are javadoc-umented.

Some commands have aliases. Example: tree <=> dump. You can define aliases in
your annotation. Example:

@Cmd(syntax="dump|tree", synopsis="Dump a subtree")
public class DumpTree extends ChemistryCommand {
...

I will add more explanations about the command syntax. (Needed to be provided
when creating new commands).

