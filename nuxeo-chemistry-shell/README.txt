About this project
==================

This is a shell (command-line tool for you Windows people) that let you browse
and manage content on a CMIS repository (a bit like Cadaver for WebDAV, if you
know it).

Building
--------

To build this project, just run "mvn install". You probably want to install
first Apache Chemistry, by checking out the sources and running "mvn install"
there, since it is not yet released.

If you have make on you system, you can also use the following make targets:
clean, build, test and release.

Dependencies
------------

The project doesn't depend on the Nuxeo EP code (so it could be pushed into the
Apache Chemistry codebase if wanted).

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
for demonstration (you can change this in color.properties).

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

To register a new command you need to update the constructor of ChemistryApp:

public ChemistryApp() {
    registry.registerCommand(new DumpTree());
    ...
}

Command registration will be automized in future.

The most important interfaces you need to know about are Application and
Context which are javadoc-umented.

Some commands have aliases. Example: tree <=> dump. You can define aliases in
your annotation. Example:

@Cmd(syntax="dump|tree", synopsis="Dump a subtree")
public class DumpTree extends ChemistryCommand {
...


Command Syntax
--------------

When defining new commands we need to add the @Cmd annotation on the command
class.

This annotation provides 2 command properties: syntax and synopsis

- The synopsis is a short description that is listed on the right side of the
  command when you print the commands list using 'cmds'

- The syntax is important and define the command line structure and how
  auto-completion will be done.

Here is the syntax format:

cmd_name param_spec param_spec  ...
cmd_name: name1 | name 2 | ...  - You can have multiple names associated to a command (the first one is the command name the others are aliases)
param_spec is a parameter specification.

A parameter has a key (and 0 or more aliases), a default value, an optional flag and a type.

The type is important if you need auto-completion. There are several recognized types for now:

1. command  - a command (to complete with available command names)
2. file     - a file (to complete with a file path)
3. dir      - a directory (to complete with directory paths)
4. item     - a remote object (to complete with remote object paths)

Also a parameter can be an argument, a flag or a key/value pair. Flags are not
yet supported (i.e. -param without a value).

An optional parameter must be enclosed in brackets '[' ']'.

Optional parameters can have default values that can be specified by appending
?the_default_value after the command spec.

Example: [-d|--depth?1]

The type is optional and is specified after the command name list separated by
a ':'

Example: [targetFile:file?/tmp/some_file]

Types parameters will be auto-completed with possible values when hiting tab
key in shell.

A complete example:

> print|pr [-p|--pretty?true] [-v|--verbose?false] document:item [targetFile:file?out.txt]

An instance of that command will be types on the command line like this:

> print --verbose true MyDocuments/Doc1 /tmp/doc1.out

Invalid characters like spaces must be escaped using backslashes ('\').

Example:

> print --verbose true My\ Documents/Doc1 /tmp/doc1.out

