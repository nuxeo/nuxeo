# Nuxeo platform 8.10-HF31

Welcome to the Nuxeo Platform 8.10-HF31.

You can always download the latest releases of Nuxeo packages (distributions, 
installers, virtual machine images, ...) from [http://www.nuxeo.com/downloads/][1].
Previous releases are available at [http://nuxeo.github.com/downloads.html][2]

## Requirements

The applications based on the Nuxeo Platform need Java. Please install and set up a
Java Development Kit 7 or 8 (JDK 7 or 8, also called Java 1.7 and Java 1.8; JRE is not enough)* if it's not
already installed on your computer.  
* We currently support Oracle's JDK[3] and OpenJDK[4] as a default Java setup. 
Don't hesitate to contact us if you need us to support a JDK from another vendor.

Even if not supported, we welcome your feedback if you encounter any trouble with 
earlier versions of the JDK. Nuxeo won't work with any previous release of Java (e.g. Java 6).

Under Windows, virus scan should be configured to avoid scanning
Nuxeo's installation folder.  See [lock problems leading to server errors][5].

Read the [Hardware and Software Requirements][34] for more information.

## Getting Started

After you have installed the Nuxeo Platform, either follow the Quick configuration
steps described on the online [Installation and Administration Guide][6] or the instructions below:

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
5. Select the modules you want to install on the Platform (a lot of other modules 
   are available in [the Update Center][9] and on [the Nuxeo Marketplace][10]).
6. When the server is restarted, log in using the "Administrator"
   login and the "Administrator" password (no quotes, of course, but
   capital "A").

For complete setup and configuration, you should refer to the [Setup pages of
the Installation and Administration Guide][11].

## Complementary Features

The Nuxeo Platform uses some third party software for complementary features. Check out [Installing and Setting up Related Software][35] for the complete list.

We also provide some extensions:
* Drag and drop plugins: [Firefox][17], [Internet Explorer][18].
* Desktop integration thanks to LiveEdit: see the [plugins
  documentation page][19].

## About Nuxeo

Nuxeo provides an [Open Source Content Management Platform][21] enabling
architects and developers to easily build, deploy, and run
content-centric  business applications. In the cloud or on premise,
Nuxeo's Enterprise Content Management (ECM) technology offers an
integrated solution for [Document Management][22], [Digital Asset Management][23],
[Case Management][24] and much more. Built on a modern,
Java-based architecture, the Nuxeo Platform is architected for modularity
and extensibility, unlike traditional ECM solutions. This means that
your content-centric application aligns with your business and technical
needs and easily integrates into your IT infrastructure, all in a highly
sustainable way.

The Nuxeo Platform is a robust, extensible, global content management
platform available as Open Source Software (OSS). Nuxeo imagined,
developed and is releasing its platform, helped by a vibrant
community of professional and individual contributors:

* Capture: With the Nuxeo Platform, you can create your documents
 using your day-to-day favorite tools (MS Office, Mail, etc.) or
 you can batch import your exiting documents (drag and drop, form
 and import, email capture).
* Share & Collaborate: Save time by finding the right information
 when you need it! Empower your co-workers to share, access, enrich
 and store high quality content in a single place. The Nuxeo
 Platform can manage all of your organization's documents and IP
 assets in a scalable and secured document repository. Create and
 enrich documents within collaborative workspaces, follow their
 validation process via customizable workflows, publish in one or
 any hierarchy you chose. Join our existing customers, take all the
 benefits from our collaborative and open source content management
 platform.
* Process & Review: Customize your documents life cycle, quickly
 adapt workflows to reflect internal procedures and to meet your
 legal compliancy requirements. The Nuxeo Platform can handle the
 way you work!
* Publish & Archive: Keep track of any and all changes that have
 happened within the document repository. The Nuxeo Platform allows
 you to publish the validated documents you have created to a larger
 audience, and the audit trail offers the legal compliance that your
 organization requires.
* Search & Find: With the Nuxeo Platform search features, you can
 easily find your documents through their content or their metadata.
 Save time: you can save and reuse your most frequent queries.

## Release Notes

See [http://nuxeo.github.io/releasenotes/8.10-HF31/][25].

## Issues

Please go to the [JIRA issue tracker][26] for the Nuxeo Platform
project to check the [fixed and currently known bugs and issues
with this release][27].

## Documentation and Help

You will find documentation on the [the Nuxeo community site][28].

More specifically:

* The [Nuxeo User Guide][29]
* [Customization and Development with Nuxeo][30]
* The [Nuxeo Technical Documentation Center][31]
* The [Nuxeo Administration Guide][32]

## Licenses

The Nuxeo Platform is copyright 2006-2015 Nuxeo SA. It is released under the Apache License, Version 2.0.
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
Portuguese: Jose Luis de la Rosa
Brazilian: Rogerio J. Gentil, Klyff Harlley
Russian: Jane Smorodnik
Spanish: Daniel Tellez, Jose Luis de la Rosa
Serbian - Cyrillic: Goran Ljubic
Vietnamese: Le Tuan Dat, Daniel Tellez
Czech: Miroslav Lednicky

This release contains open source libraries developed by the JBoss
Group, the Apache Foundation, Sun Microsystems, HP, the OSGi Alliance,
the JMock project, the JFlex project, the Cup project, etc.

This project is developed using tools that include: Eclipse, JUnit,
Ant, Maven, JIRA, Hudson, YourKit Java Profiler (thanks to free
licenses donated by YourKit), etc.


[1]: http://www.nuxeo.com/downloads/
[2]: http://nuxeo.github.com/downloads.html
[3]: http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html
[4]: http://openjdk.java.net/install/index.html
[5]: https://doc.nuxeo.com/x/ngQz
[6]: https://doc.nuxeo.com/x/G4AO
[7]: http://doc.nuxeo.com/x/NIpH
[8]: http://doc.nuxeo.com/x/6QJc
[9]: http://localhost:8080/nuxeo/nxhome/default@view_admin?tabIds=NUXEO_ADMIN%3AConnectApps
[10]: https://marketplace.nuxeo.com/
[11]: https://doc.nuxeo.com/x/PwA7
[17]: https://download.nuxeo.com/browser/firefox/nuxeo-dragdrop-ff-extension.xpi
[18]: http://download.nuxeo.org/desktop-integration/drag-drop/msie/
[19]: http://doc.nuxeo.com/x/K4Wo
[21]: http://www.nuxeo.com/en/products/enterprise-platform/
[22]: http://www.nuxeo.com/en/products/document-management
[23]: http://www.nuxeo.com/en/products/dam
[24]: http://www.nuxeo.com/en/products/case-management
[25]: http://nuxeo.github.io/releasenotes/8.10-HF31
[26]: http://jira.nuxeo.org/browse/NXP?report=com.atlassian.jira.plugin.system.project:versions-panel
[27]: https://jira.nuxeo.org/browse/NXP/fixforversion/
[28]: http://doc.nuxeo.com/
[29]: http://doc.nuxeo.com/x/aYEk
[30]: http://doc.nuxeo.com/x/E4AO
[31]: https://doc.nuxeo.com/x/PIAO
[32]: https://doc.nuxeo.com/x/G4AO
[34]: http://doc.nuxeo.com/x/OwA7
[35]: http://doc.nuxeo.com/x/zgJc
