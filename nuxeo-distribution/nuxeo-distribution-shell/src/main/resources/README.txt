Use nxclient.sh or nxshell.sh
'./nxshell.sh' is equivalent to './nxclient.sh interactive'

Usage:
    ./nxclient.sh [context] [command] [option]...
    ./nxshell.sh [option]...

Context:
    -dev
        Developer mode (JVM listening for transport dt_socket at address: 8788).
        Equivalent to -clear -console and starting debug server on port 8788.
    -clear
        Clear classpath cache
    -console
        Console mode. Incompatible with use of command in command-line.
        Cannot be used with developer mode (-dev).

Command:
    Any valid Nuxeo Shell command (such as 'ls', '--script file.js', ...).
    Cannot be used with console mode.
    
    debug: 
        Print the result of the command line parser.
        
    commands:
        Print the list of available commands.
        
    help:
        Print Help.
        
    reload:
        Reload command scripts.
        
    interactive:
        Enter interactive mode.
        
    script:
        Executes scripts.
        
    ls:
        List directory content (on remote).
        
    tree:
        List directory content and subdirectory content (on remote), represented 
        by a tree.
        
    cd:
        Change directory (on remote).
        
    pwd:
        Display the current path (on remote).
        
    useradd:
        Add a user, or a users list through a CSV file, in the members directory.
        
    groupmod:
        Add or set a user, or a users list through a CSV file, to an existing group.
        
    rm:
        Remove specified (or current) document.
        
    mkdir:
        Create a document of the specified type (or Folder) at the given path (or 
        in the current path).
        
    fsimport:
        Create a new file document from a local file.
        
    mtfsimport:
        Create a new file document from a local file (multi-threaded operation).
        
    repostats:
        Gather statistics on the repository.
        
    index:
        Re-index database.
        
    view:
        View the info about a document (on remote).
        
    addlocalace:
        For the current document, add a local ACE. Takes three  parameters: the 
        username, the permission and a boolean (grant if true, deny if false).
        
    viewlocalacl:
        For the current document, view the local acl.
    
    rmlocalace:
        For the current document, remove a local ACE.
        
    import:
        Import data into the repository.
        
    export:
        Export data from a repository.
        
    services:
        List services.
        
    serviceinfo:
        Show detailed information about registered components.
        
    log:
        Dynamically define a logger and its parameters (output local file, log 
        level, package or class filtered). 
        Start or stop debug mode.
        
    connect:
        (Re-)initialize connection.
        
    disconnect:
        Close current connection if any.
        
Options:
    -h, --host server_ip
        Connect on Nuxeo Core listening on server_ip. 
        
    -u, --username
        The username to use when connecting. If not specified the 'system' user 
        will be used.
        
    -P, --password
        The password to use when connecting.
        
    -h, --host
        The host where to connect to. 
        By default no host is used; this will force a local connection (in VM 
        repository). In most cases, use '-h localhost'.
        
    -p, --port
        The port where to connect to. By default '62474' is used.
        
    -d, --debug
        Debug mode. Logging switched from INFO to DEBUG. See 'log' command for 
        more options about logging.
        
For Windows users, nxclient.cmd is quite equivalent to nxclient.sh
