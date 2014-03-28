Nuxeo translation sandbox
=========================

This module contains community-created translations, as well as some basic
tools to help manage these translations.

How to update an existing translation?
--------------------------------------

If you want to participate to the translation process, please read the [How to translate the Nuxeo Platform](http://doc.nuxeo.com/x/dAQz) page.


The rest of this readme is now for developers that can't use Crowdin for some reason.


How to add a new language?
---------------------------

If your language is not on Crowdin and you cannot wait for the next Fast Track release, you'll have to do the following:

1. Take the messages.properties file from a running Nuxeo server. It should be located in 

    yourNuxeoServer/nxserver/nuxeo.war/WEB-INF/classes/messages.properties

and copy it to messages_xx_XX.properties where xx_XX is the 4 letters codename for
your language.

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

Where to add your existing translation?
--------------------------------------

Here's the resolving order when looking for a label in Brazillian for instance.

messages_pt_BR.properties -> messages_pt_PT.properties -> messages_en.properties -> messages.properties

Brazillian is a 'dialect' of Portuguese, so there is first a fallback on Portuguese, then a fallback to the default language of the application (en for Nuxeo) then to messages.properties. 

Most of the fallback are actually handle directly by Crowdin, the tool we use for translations. When downloading a file from Crowdin, like messages_pt_BR.properties for instance, the missing labels will be replaced by the one in messages_pt_PT.properties if it exists then by the reference english file used by Crowdin. This is why you'll see english translations in other languages files.

What's with these 2 letter files like messages_pt.properties? Well those are actually an automatic copy of the four letter version. It's only here to have a two letter fallback when browsers language are set to a two letter format. So you are not suppose to modify them, ever. 

How to use add custom translation to an existing language?
----------------------------------------------------------

If you want to add your custom labels translation to an existing language (usualy because it does not fit the generic use we have of said label):

1. Take your messages_xx_XX.properties where xx_XX is the 4 letters codename for your language.

2. Create a Nuxeo Bundle and put your file under
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

