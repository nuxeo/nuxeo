# Nuxeo external translations

This module contains community-created translations, as well as some basic
tools to help manage these translations.

## How to update an existing translation?

If you want to participate to the translation process, please read the
[How to translate the Nuxeo Platform](http://doc.nuxeo.com/x/dAQz)
page.

The rest of this README is for developers that are maintaining this
module according to Crowdin translation files. Note there is some
[tooling used to handle this
module](https://github.com/nuxeo/tools-nuxeo-crowdin/).

## WARNING

All properties files in this module are managed automatically: except
on edge cases, you should never push changes to these files on GitHub,
otherwise they may be lost at next automated synchronization.

Note that only the master branch is handled by synchronization, manual
changes are still needed on maintenance branches.


## How to add a new language?

If your language is not on Crowdin, create it on Crowdin, and download
the corresponding file.

1. Set the new language as a new target language on Crowdin, and download
   the corresponding file.

   Rename this translation file to messages_xx_XX.properties, where xx_XX is the 4 letters
codename for your language, and reference it in the crowdin.ini file.

2. Create a Nuxeo Bundle and put your file under

    src/main/resources/web/nuxeo.war/WEB-INF/classes/

3. Modify the deployment-fragment.xml file accordingly:

    <?xml version="1.0"?>
    <fragment version="1">
      <require>org.nuxeo.ecm.platform.lang.ext</require>

      <extension target="faces-config#APPLICATION_LOCALE">
        <locale-config>
          <supported-locale>xx_XX</supported-locale><!-- Your custom locale -->
        </locale-config>
      </extension>

      <install>
        <!-- Unzip the contents of our nuxeo.war into the real nuxeo.war on the server -->
        <unzip from="${bundle.fileName}" to="/" prefix="web">
          <include>web/nuxeo.war/**</include>
        </unzip>

        <!-- Add fallback to two letters locale for browser compatibility if needed -->
        <copy from="nuxeo.war/WEB-INF/classes/messages_xx_XX.properties"
            to="nuxeo.war/WEB-INF/classes/messages_xx.properties"/>

      </install>

    </fragment>


## Where to add your existing translations?

Here's the resolving order when looking for a label in Brazilian for
instance.

messages_pt_BR.properties -> messages_pt_PT.properties -> messages_en.properties -> messages.properties

Brazilian is a 'dialect' of Portuguese, so there is first a fallback
on Portuguese, then a fallback to the default language of the
application ("en" for Nuxeo) then to messages.properties.

Most of the fallback is actually handled directly by Crowdin, the tool
we use for translations. When downloading a file from Crowdin, like
messages_pt_BR.properties for instance, the missing labels will be
replaced by the ones from file messages_pt_PT.properties (if it
exists), then by the reference English file used by Crowdin. This is
why you'll see English translations by default in some non-English
files.

What's with these 2 letter files like messages_pt.properties? Well
those are actually an automatic copy of the four letter version. It's
only here to have a two letter fallback when browsers language are set
to a two letter format. So you are not suppose to modify them, ever.


## How to add custom translations to an existing language?

If you want to add your custom label translations to an existing
language, you can contribute it to the main file holding all
translations.

1. Take your messages_xx_XX.properties where xx_XX is the 4 letters
   codename for your language.

2. Create a Nuxeo Bundle and put your file under:

    src/main/resources/web/nuxeo.war/WEB-INF/classes/

3. Modify the deployment-fragment.xml file accordingly:

    <?xml version="1.0"?>
    <fragment version="1">
      <require>org.nuxeo.ecm.platform.lang.ext</require>

      <install>
        <delete path="${bundle.fileName}.tmp" />
        <mkdir path="${bundle.fileName}.tmp" />

        <unzip from="${bundle.fileName}" to="${bundle.fileName}.tmp" />

        <!-- Add the content of messages_xx_XX.properties at the end of the existing file. -->
        <append from="${bundle.fileName}.tmp/web/nuxeo.war/WEB-INF/classes/messages_xx_XX.properties"
          to="nuxeo.war/WEB-INF/classes/messages_xx_XX.properties" addNewLine="true" />
        <!-- Add fallback to two letters locale for browser compatibility if needed. -->
        <append from="${bundle.fileName}.tmp/web/nuxeo.war/WEB-INF/classes/messages_xx_XX.properties"
          to="nuxeo.war/WEB-INF/classes/messages_xx.properties" addNewLine="true" />

        <delete path="${bundle.fileName}.tmp" />
      </install>

    </fragment>


## How to override existing translations?

The same procedure as above can be used to override some existing
translations. Just make sure you also require any bundle that would
define them (like bundle "org.nuxeo.ecm.platform.lang.ext" above): the
last definition in the file wins over the others.
