                  Nuxeo platform 5.4.2-HF10 Release Notes

   Welcome to the Nuxeo platform 5.4.2-HF10.

   You can always download the latest releases of Nuxeo applications based
   on the Nuxeo Enterprise Platform from
   [1]http://www.nuxeo.com/en/downloads.

Requirements

   The applications based on Nuxeo EP 5.4 need Java. Please install and
   set up the recommended [2]Sun Java Development Kit (JDK) 6 if it's not
   already installed on your computer.

   Nuxeo is also compliant with the older Java 5, but support for it is
   likely to be dropped in a future release.

   We've also had success running Nuxeo DM on the OpenJDK 6 (the default
   version of Java installed on most Linux distributions). We don't
   consider the OpenJDK as a supported platform yet, but we welcome your
   feedback if you encounter any trouble with it.

   It won't work with any previous release of Java (e.g. Java 1.4).

   Under Windows, virus scan should be configured to avoid scanning
   Nuxeo's installation folder. More details on [3]this FAQ entry .

Getting Started

   After you have installed the Nuxeo application:

   Either follow the Quick installation steps described on the online
   [4]Installation and Administration Guide or the instructions below.
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
    5. When the server is restarted, log in using the "Administrator"
       login and the "Administrator" password (no quotes, of course, but
       capital "A").

   For setup and configuration, you should refer to the [5]Setup pages of
   the Installation and Administration Guide.

Complementary features

   The following features depend on the environment where you installed
   your Nuxeo application.
     * PDF transformation: this feature requires OpenOffice started as
       server, complete the installation by following [6]the OpenOffice
       instructions on the related software page.
     * Preview and annotations: requires install of third-party software
       [7]"pdftohtml", see [8]the pdftohtml instructions on the related
       software page.
     * Image tiling: requires install of third-party software [9]"Image
       Magick", see [10]the Image Magick instructions on the related
       software page.
     * Drag and drop plugins: [11]Firefox, [12]Internet Explorer.
     * Desktop integration thanks to LiveEdit: see the [13]plugins
       download page.

About Nuxeo applications

   Nuxeo applications are robust, extensible, global content management
   solutions available as Open Source Software (OSS). Nuxeo imagined,
   developed and is releasing its applications, helped by a vibrant
   community of professional and individual contributors.
     * Capture: With Nuxeo applications, you can create your documents
       using your day-to-day favorite tools (MS Office, Mail, etc.) or or
       you can batch import your exiting documents (drag and drop, form
       and import, email capture).
     * Share & Collaborate: Save time by finding the right information
       when you need it ! Empower your co-workers to share, access, enrich
       and store high quality content in a single place. Nuxeo
       applications can manage all of your organization's documents and IP
       assets in a scalable and secured document repository. Create and
       enrich documents within collaborative workspaces, follow their
       validation process via customizable workflows, publish in one or
       any hierarchy you chose. Join our existing customers, take all the
       benefits from our collaborative open source content management
       solutions.
     * Process & Review: Customize your documents life cycle, quickly
       adapt workflows to reflect internal procedures and to meet your
       legal compliancy requirements. Nuxeo applications can handle the
       way you work!
     * Publish & Archive: Keep track of any and all changes that have
       happened within the document repository. Nuxeo applications allow
       you to publish the validated documents you have created to a larger
       audience, and the audit trail offers the legal compliance that your
       organization requires.
     * Search & Find: With the Nuxeo applications search features, you can
       easily find your documents through their content or their metadata.
       Save time: you can save and reuse your most frequent queries.

   This release note presents the main features of the platform, through a
   default web client.

Known Issues

   Please go to the [14]Jira issue tracker for the Nuxeo ECM Platform
   project on jira.nuxeo.org to check the [15]currently bugs and issues
   with this release.

Documentation and Help

   You will find documentation on the [16]the Nuxeo community site.

   More specifically :
     * The [17]Nuxeo User Guide
     * How to develop with Nuxeo - [18]The Tutorial (draft)
     * The [19]Nuxeo Technical Documentation Center
     * The [20]Nuxeo Administration Guide

Licenses

   Nuxeo EP and Nuxeo DM are copyright 2006-2011 Nuxeo SA. They are
   released under the LGPL license. Nuxeo EP includes third-party
   libraries, licensed under compatible open source licenses.

Acknowledgments

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

About Nuxeo

   Nuxeo provides a modular, extensible Java-based [21]open source
   software platform for enterprise content management and packaged
   applications for [22]document management, [23]digital asset management
   and [24]case management. Designed by developers for developers, the
   Nuxeo platform offers modern architecture, a powerful plug-in model and
   extensive packaging capabilities for building content applications.

References

   1. http://www.nuxeo.com/en/downloads
   2. http://java.sun.com/javase/downloads/index.jsp
   3. https://doc.nuxeo.com/x/ngQz
   4. https://doc.nuxeo.com/x/UQJc
   5. https://doc.nuxeo.com/x/PwA7
   6. https://doc.nuxeo.com/x/zgJc
   7. http://sourceforge.net/projects/pdftohtml/
   8. https://doc.nuxeo.com/x/zgJc
   9. http://www.imagemagick.org/script/index.php
  10. https://doc.nuxeo.com/x/zgJc
  11. https://download.nuxeo.com/browser/firefox/nuxeo-dragdrop-ff-extension.xpi
  12. http://download.nuxeo.org/desktop-integration/drag-drop/msie/
  13. http://www.nuxeo.com/en/downloads/desktop-integration
  14. http://jira.nuxeo.org/browse/NXP?report=com.atlassian.jira.plugin.system.project:versions-panel
  15. https://jira.nuxeo.org/browse/NXP/fixforversion/
  16. http://doc.nuxeo.com/
  17. http://doc.nuxeo.org/current/books/nuxeo-user-guide/html/
  18. http://doc.nuxeo.org/current/books/nuxeo-learning/html/
  19. https://doc.nuxeo.com/x/PIAO
  20. https://doc.nuxeo.com/x/G4AO
  21. http://www.nuxeo.com/en/products/ep
  22. http://www.nuxeo.com/en/products/document-management
  23. http://www.nuxeo.com/en/products/dam
  24. http://www.nuxeo.com/en/products/case-management
