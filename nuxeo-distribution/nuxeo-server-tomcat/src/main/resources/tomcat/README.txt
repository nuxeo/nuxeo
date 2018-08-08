# Nuxeo platform 9.10-HF14

Welcome to the Nuxeo Platform 9.10-HF14.

You can always download the latest releases of Nuxeo packages (distributions, 
installers, virtual machine images, ...) from [https://www.nuxeo.com/downloads/][1].
Previous releases are available at [http://nuxeo.github.com/downloads.html][2]

## Requirements

The applications based on the Nuxeo Platform need Java. Please install and set up a
Java Development Kit 8 (JDK 8, also called Java 1.8; JRE is not enough)* if it's not
already installed on your computer.  
* We currently support Oracle's JDK[3] and OpenJDK[4] as a default Java setup. 
Don't hesitate to contact us if you need us to support a JDK from another vendor.

Even if not supported, we welcome your feedback if you encounter any trouble with 
earlier versions of the JDK. Nuxeo won't work with any previous release of Java.

Under Windows, virus scan should be configured to avoid scanning
Nuxeo's installation folder.  See [lock problems leading to server errors][5].

Read the [Installation documentation][34] for more information.

## Getting Started

After you have installed the Nuxeo Platform, either follow the Quick configuration
steps described on the online [Installation Guide][34] or the instructions below:

1. Go to your installation directory.
2. Start the server:
  + under Linux/Unix/MacOS, run "./bin/nuxeoctl start" to use only the terminal 
    or "./bin/nuxeoctl gui start" to display the Control Panel 
    (run "./bin/nuxeoctl help" for list of available commands).
  + under Windows, run "bin\nuxeoctl.bat" (the Control Panel is displayed by default, 
    use "--gui=false" option for terminal only).
  + you can also use a Debian[7] or Windows[8] service.  
3. Point your browser to http://localhost:8080/nuxeo/.
4. Configure your server using the displayed Startup Wizard.
5. Select the addons you want to install on the Platform (a lot of other addons 
   are available on [the Nuxeo Marketplace][10]).
6. When the server is restarted, log in using the "Administrator"
   login and the "Administrator" password (no quotes, of course, but
   capital "A").

For complete setup and configuration, you should refer to the [Administration pages of
the documentation][11].

## Complementary Features

The Nuxeo Platform uses some third party software for complementary features. Check out [Installing and Setting up Related Software][35] for the complete list.

## About Nuxeo

Nuxeo, maker of the leading, cloud-native content services platform, is reinventing enterprise content and digital asset management. Nuxeo is fundamentally changing how people work with both data and content to realize new value from digital information. Its cloud-native, hyper-scalable content services platform has been deployed by large enterprises, mid-sized businesses and government agencies worldwide. Customers like Verizon, Boeing, Electronic Arts, and the US Department of Defense have used Nuxeo's technology to transform the way they do business. Founded in 2008, the company is based in New York with offices across the United States and Europe. Additional information is available at [https://www.nuxeo.com/][21].

## Release Notes

See [https://doc.nuxeo.com/nxdoc/nuxeo-server-release-notes/][25].

## Issues

Please go to the [JIRA issue tracker][26] for the Nuxeo Platform
project to check the [fixed and currently known bugs and issues
with this release][27].

## Documentation and Help

You will find documentation on [https://doc.nuxeo.com/]][28].

More specifically:

* The [Nuxeo User Guide][29]
* [Tutorials and How-Tos][30]
* The [Nuxeo Server documentation][31]
* The [Clients and SDKs documentation][32]
* [Tools for the Nuxeo Platform documentation][36]

## Licenses

The Nuxeo Platform is copyright 2006-2017 Nuxeo. It is released under the Apache License, Version 2.0.
The Nuxeo Platform includes third-party libraries, licensed under compatible open source licenses.

## Acknowledgments

This release contains code developed by the Nuxeo Team, the Leroy Merlin team 
and Jean-Marc from Chalmers University in Goteborg.
Many more people have suggested improvements, spotted issues or contributed patches.

The following translations are contributions from the community:

Arabic: Taieb Felfel
Basque: Jose Luis de la Rosa
Catalan: Jose Luis de la Rosa, Jordi Mallach 
Chinese: Line Lu
Dutch: Capgemini NL Team
English: Nuxeo Team
French: Nuxeo Team
French (Canada) : Patrick Turcotte
Galician: Jose Luis de la Rosa
German: Georges Racinet, Edgar Geisler
Greek: Nina Bagouli
Italian: Mirto Silvio Busico, Samuele Innocenti, Stefane Fermigier
Japanese: Damien Dupraz, Hiromi Kimura
Polish: Adam Lozy
Portuguese: Jose Luis de la Rosa, Nuxeo Team
Brazilian: Rogerio J. Gentil, Klyff Harlley
Russian: Jane Smorodnik
Spanish: Daniel Tellez, Jose Luis de la Rosa, Nuxeo Team
Serbian - Cyrillic: Goran Ljubic
Vietnamese: Le Tuan Dat, Daniel Tellez
Czech: Miroslav Lednicky

This release contains open source libraries developed by the JBoss
Group, the Apache Foundation, Sun Microsystems, HP, the OSGi Alliance,
the JMock project, the JFlex project, the Cup project, etc.

This project is developed using tools that include: Eclipse, JUnit,
Ant, Maven, JIRA, Hudson, YourKit Java Profiler (thanks to free
licenses donated by YourKit), etc.


[1]: https://www.nuxeo.com/downloads/
[2]: http://nuxeo.github.com/downloads.html
[3]: http://www.oracle.com/technetwork/java/javase/downloads/index.html
[4]: http://openjdk.java.net/install/index.html
[5]: https://doc.nuxeo.com/nxdoc/adding-an-antivirus/
[7]: https://doc.nuxeo.com/nxdoc/configuring-the-nuxeo-platform-as-a-daemon-with-sysvinit/
[8]: https://doc.nuxeo.com/nxdoc/installing-the-nuxeo-platform-as-a-windows-service/
[10]: https://marketplace.nuxeo.com/
[11]: https://doc.nuxeo.com/nxdoc/administration/
[17]: https://download.nuxeo.com/browser/firefox/nuxeo-dragdrop-ff-extension.xpi
[18]: http://download.nuxeo.org/desktop-integration/drag-drop/msie/
[21]: https://www.nuxeo.com/
[25]: https://doc.nuxeo.com/nxdoc/nuxeo-server-release-notes/
[26]: https://jira.nuxeo.com/browse/NXP
[27]: https://jira.nuxeo.com/browse/NXP/fixforversion/
[28]: https://doc.nuxeo.com/
[29]: https://doc.nuxeo.com/userdoc/
[30]: https://doc.nuxeo.com/nxdoc/getting-started/
[31]: https://doc.nuxeo.com/nxdoc/nuxeo-server/
[32]: https://doc.nuxeo.com/nxdoc/client-sdks/
[34]: https://doc.nuxeo.com/nxdoc/installation/
[35]: https://doc.nuxeo.com/nxdoc/installing-and-setting-up-related-software/
[36]: https://doc.nuxeo.com/nxdoc/nuxeo-tools/
