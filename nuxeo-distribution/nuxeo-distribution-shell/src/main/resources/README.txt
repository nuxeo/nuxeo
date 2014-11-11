Use nxclient.sh or nxshell.sh
'./nxshell.sh' is equivalent to './nxclient.sh interactive'

Usage:
    ./nxclient.sh [-dev] [-h server_ip] [-d]
    ./nxclient.sh [-clear] [-console] [-h server_ip] [-d]
    ./nxclient.sh [-clear] [command] [-h server_ip] [-d]
    ./nxclient.sh [-dev] [-clear] [-console] [command] [-h server_ip] [-d]
    ./nxshell.sh [-h server_ip] [-d]

Options:
    -dev
        Developer mode (JVM listening for transport dt_socket at address: 8788).
        Equivalent to -clear -console and starting debug server on port 8788.
    -clear
        Clear classpath cache
    -console
        Console mode. Incompatible with use of command in command-line.
    command
        Any valid Nuxeo Shell command (such as 'ls', '--script file.js', ...).
        See http://doc.nuxeo.org/5.1/books/nuxeo-book/html/nuxeo-shell.html
        Cannot be used with console mode.
    -h server_ip
        Connect on Nuxeo Core listening on server_ip. Use at least '-h localhost'.
    -d
        Debug mode. Logging switched from INFO to DEBUG.
        See 'log' command for more options about logging.

For Windows users, nxclient.cmd is quite equivalent to nxclient.sh
