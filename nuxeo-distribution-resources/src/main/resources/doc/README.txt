# Nuxeo platform 5.6-SNAPSHOT Release Notes

Welcome to the Nuxeo Platform 5.6-SNAPSHOT.

You can always download the latest releases of Nuxeo modules
on the Nuxeo Platform from
[http://www.nuxeo.com/en/downloads.][1]

## Requirements

The applications based on Nuxeo EP 5.5 need Java. Please install and
set up the recommended [Sun Java Development Kit (JDK) 6][2] if it's not
already installed on your computer.

Nuxeo Platform 5.5 supports OpenJDK 6 as a default Java setup.
We don't consider Java 7 as a supported version yet, but we welcome
your feedback if you encounter any trouble with it.

It won't work with any previous release of Java (e.g. Java 5).

Under Windows, virus scan should be configured to avoid scanning
Nuxeo's installation folder. More details on [this FAQ entry][3].

## Getting Started

For Internet Explorer 9 users:
You need to add the Nuxeo server URL in the trusted sites list to be
able to complete the installation and configuration steps.
In the Internet Options > Security > Trusted Sites menu, click on
the Sites button, type the Nuxeo server URL and add it.

After you have installed the Nuxeo Platform:

Either follow the Quick installation steps described on the online
[Installation and Administration Guide][4] or the instructions below:

1. Go to your installation directory.
2. Start the server:
  + under Linux/Unix/MacOS, run "./bin/nuxeoctl start" to use only
    the terminal or "./bin/nuxeoctl gui start" to display the
    Control Panel (run "./bin/nuxeoctl help" for list of available
    commands).
  + under Windows if you installed Nuxeo from the .zip file, run
    "bin\nuxeoctl.bat".
  + under Windows if you installed Nuxeo from the .exe file,
    either run "bin\nuxeoctl.bat" or use the "nuxeo" Windows
    service.
3. Point your browser to http://localhost:8080/nuxeo/.
4. Configure your server using the displayed Startup Wizard.
5. Select the modules you want to install on the Platform.
6. When the server is restarted, log in using the "Administrator"
   login and the "Administrator" password (no quotes, of course, but
   capital "A").

For setup and configuration, you should refer to the [Setup pages of
the Installation and Administration Guide][5].

## Complementary features

The following features depend on the environment where you installed
your Nuxeo application:

* PDF transformation: this feature requires OpenOffice started as
  server, complete the installation by following [the OpenOffice
  instructions on the related software page][6].
* Preview and annotations: requires install of third-party software
 [pdftohtml][7], see [the pdftohtml instructions on the related
  software page][8].
* Image tiling: requires install of third-party software [ImageMagick][9],
 see [the ImageMagick instructions on the related software page][10].
* Drag and drop plugins: [Firefox][11], [Internet Explorer][12].
* Desktop integration thanks to LiveEdit: see the [plugins
 download page][13].
* Video features: requires install of third-party software FFmpeg:
 for [Windows][14] and [other OS][15].


## About Nuxeo

Nuxeo provides an [Open Source Content Management Platform][16] enabling
architects and developers to easily build, deploy, and run
content-centric  business applications. In the cloud or on premise,
Nuxeo's Enterprise Content Management (ECM) technology offers an
integrated solution for [Document Management][17], [Digital Asset Management][18],
[Case Management][19] and much more. Built on a modern,
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
 using your day-to-day favorite tools (MS Office, Mail, etc.) or or
 you can batch import your exiting documents (drag and drop, form
 and import, email capture).
* Share & Collaborate: Save time by finding the right information
 when you need it ! Empower your co-workers to share, access, enrich
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

This release note presents the main features of the platform, through a
default web client.

## Known Issues

Please go to the [Jira issue tracker][20] for the Nuxeo ECM Platform
project on jira.nuxeo.org to check the [currently bugs and issues
with this release][21].

## Documentation and Help

You will find documentation on the [the Nuxeo community site][22].

More specifically:

* The [Nuxeo User Guide][23]
* [Customization and Development with Nuxeo][24]
* The [Nuxeo Technical Documentation Center][25]
* The [Nuxeo Administration Guide][26]

## Licenses

The Nuxeo Platform is copyright 2006-2011 Nuxeo SA. They are
released under the LGPL license. The Nuxeo Platform includes third-party
libraries, licensed under compatible open source licenses.

## Acknowledgments

This release contains code developed by the Nuxeo Team (Alain, Anahide,
Benjamin, Bogdan, Eric, Eugen, Julien, Lise, Olivier, Solen, Stefane,
Thierry, Tibo...), the Leroy Merlin team and Jean-Marc from Chalmers
University in Goteborg. Many more people have suggested improvement,
spotted issues or contributed bugfixes on the mailing list.

The following translations are contributions from the community:

Arabic: Taieb Felfel
Basque: Jose Luis de la Rosa
Catalan: Jose Luis de la Rosa
Chinese: Line Lu
English: Nuxeo Team
French: Nuxeo Team
Galician: Jose Luis de la Rosa
German: Georges Racinet
Greek: Nina Bagouli
Italian: Mirto Silvio Busico, Samuele Innocenti, Stefane Fermigier
Japanese: Damien Dupraz
Polish: Adam Lozy
Portuguese: Jose Luis de la Rosa
Brazilian: Rogerio J. Gentil
Russian: Jane Smorodnik
Spanish: Daniel Tellez, Jose Luis de la Rosa
Vietnamese: Le Tuan Dat and Daniel Tellez

This release contains open source libraries developed by the JBoss
Group, the Apache Foundation, Sun Microsystems, HP, the OSGi Alliance,
the JMock project, the JFlex project, the Cup project, etc.

This project is developed using tools that include: Eclipse, JUnit,
Ant, Maven, Jira, Hudson, YourKit Java Profiler (thanks to free
licenses donated by YourKit), etc.

[1]: http://www.nuxeo.com/en/downloads
[2]: http://www.oracle.com/technetwork/java/javase/downloads/index.html
[3]: https://doc.nuxeo.com/x/ngQz
[4]: https://doc.nuxeo.com/x/G4AO
[5]: https://doc.nuxeo.com/x/PwA7
[6]: https://doc.nuxeo.com/x/zgJc
[7]: http://sourceforge.net/projects/pdftohtml/
[8]: https://doc.nuxeo.com/x/zgJc
[9]: http://www.imagemagick.org/script/index.php
[10]: https://doc.nuxeo.com/x/zgJc
[11]: https://download.nuxeo.com/browser/firefox/nuxeo-dragdrop-ff-extension.xpi
[12]: http://download.nuxeo.org/desktop-integration/drag-drop/msie/
[13]: http://www.nuxeo.com/en/downloads/desktop-integration
[14]: http://ffmpeg.zeranoe.com/builds/
[15]: http://ffmpeg.org/download.html
[16]: http://www.nuxeo.com/en/products/enterprise-platform/
[17]: http://www.nuxeo.com/en/products/document-management
[18]: http://www.nuxeo.com/en/products/dam
[19]: http://www.nuxeo.com/en/products/case-management
[20]: http://jira.nuxeo.org/browse/NXP?report=com.atlassian.jira.plugin.system.project:versions-panel
[21]: https://jira.nuxeo.org/browse/NXP/fixforversion/
[22]: http://doc.nuxeo.com/
[23]: http://doc.nuxeo.org/current/books/nuxeo-user-guide/html/
[24]: http://doc.nuxeo.com/display/NXDOC/Customization+and+Development
[25]: https://doc.nuxeo.com/x/PIAO
[26]: https://doc.nuxeo.com/x/G4AO
