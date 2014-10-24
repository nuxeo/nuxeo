nuxeo-platform-mail
===================

## About

The `nuxeo-platform-mail` module provides the `MailFolder` feature : you can configure Nuxeo Server to fetch emails from a Pop3/Imapc server and have Nuxeo convert the emails into Nuxeo Documents and store them inside a Folder.

## Mail server connection

One important aspect of nuxeo-platform-mail consists in the fact that multiple
email connections can be dynamically configured from the web UI interface.

This is done by creating "Email folder" documents, which contain the parameters
needed in order to connect to the email account. 

Periodaically (this is configured in {{nxmail-scheduler-config.xml}}), all
the email accounts defined by the "Email folders" are checked for new incoming
mail. 

For every new mail found in a certain account, a new corresponding 
"Email message" is created as a child of the "Email folder" corresponding to
the email account.

Also, an email account check can be triggered for a certain "Email folder", 
by clicking the "Check email" button which can be found in the view page of
an "Email folder" document.

Note: For performance reasons, only a limited number of emails are
imported for every account check. This limit can be set when a the
creation/modification of a MailFolder document.

## Configuration

### Extension point configuration

Mail Servers are configured at the MailFolder level.

However, you can also control how the MailFolder will convert emails into Nuxeo Document.

The service uses a chain of actions that is configured using the `actionPipes` extension point :

The default configuration contains :

      <action>
        org.nuxeo.ecm.platform.mail.listener.action.StartAction
      </action>
      <action>
        org.nuxeo.ecm.platform.mail.listener.action.ExtractMessageInformationAction
      </action>
      <action>
        org.nuxeo.ecm.platform.mail.listener.action.CheckMailUnicity
      </action>
      <action>
        org.nuxeo.ecm.platform.mail.listener.action.CreateDocumentsAction
      </action>
      <action>
        org.nuxeo.ecm.platform.mail.listener.action.EndAction
      </action>

The action that will actually create the Document is `CreateDocumentsAction`.

So, if you want the mail folder to create custom Document types, you have to contribute your own Action and overriding the default chain.

However, if you don't want to do Java Code and prefer using Automation, since 6.0, you can.

### Automation Bridge

Since 6.0 a new MailAction is available and allows to call an Automation Chain.


#### Configuraring the system to use Automation

You must override the default actionPipe and use the `CreateDocumentsFromAutomationChainAction` action

	<?xml version="1.0"?>
	<component name="org.nuxeo.ecm.platform.mail.automation.override">
	  
	  <require>org.nuxeo.ecm.platform.mail.service.MailServiceContrib</require>
	    
	  <extension target="org.nuxeo.ecm.platform.MailService" point="actionPipes">
	    <pipe name="nxmail"  override="true">
	      <action>
		org.nuxeo.ecm.platform.mail.listener.action.StartAction
	      </action>
	      <action>
		org.nuxeo.ecm.platform.mail.listener.action.ExtractMessageInformationAction
	      </action>
	      <action chain="CreateMailDocumentFromAutomation">
		org.nuxeo.ecm.platform.mail.listener.action.CreateDocumentsFromAutomationChainAction
	      </action>
	    </pipe>

	  </extension>

	</component>

#### Automation Execution Context

The Automation context is filled with the ExecutionContext used in the MailActionPipe :

 - `mailFolder` : is the target MailFolder document
 - `mailDocumentName` : is the computed name for the Mail Document (it is not required to use it)
 - `executionContext` : is the `ExecutionContext` defined in the pipe
 - all properties available in the `ExecutionContext` are dumped at root of Automation Context :
    - `subject` is the mail subject
    - `recipients` is the list of recipients
    - `ccRecipients` is the list of recipients in CC
    - `sendingDate` is the date the mail was sent
    - `attachments` is the list of attached Blobs

#### Chain example

The Chain itself should be something like : 


    <chain id="CreateMailDocumentFromAutomation">
      <operation id="Context.RestoreDocumentInput">
        <param type="string" name="name">mailFolder</param>
      </operation>
      <operation id="Document.Create">
        <param type="string" name="type">MailMessage</param>
        <param type="string" name="name">expr:Context["mailDocumentName"]</param>
        <param type="properties" name="properties">expr:mail:messageId=@{messageId}
        </param>
      </operation>
      <operation id="Context.SetInputAsVar">
        <param type="string" name="name">mailDocument</param>
      </operation>
      <operation id="Context.RunScript">
        <param type="string" name="script">
           
           Context["mailDocument"].setPropertyValue("dc:title",Context["subject"]);
           Context["mailDocument"].setPropertyValue("mail:recipients",Context["recipients"]);
           Context["mailDocument"].setPropertyValue("mail:cc_recipients",Context["ccRecipients"]);
           Context["mailDocument"].setPropertyValue("mail:sending_date",Context["sendingDate"]);
              
        </param>
      </operation>
      <operation id="Context.RunOperationOnList">
        <param type="string" name="id">ProcessAttachment</param>
        <param type="string" name="list">attachments</param>
        <param type="boolean" name="isolate">true</param>
        <param type="string" name="item">attachment</param>
      </operation>
      <operation id="Context.RestoreDocumentInput">
        <param type="string" name="name">mailDocument</param>
      </operation>
      <operation id="Document.Save"/>
    </chain>
    <chain id="ProcessAttachment">
      <operation id="Context.RestoreBlobInput">
        <param type="string" name="name">attachment</param>
      </operation>
      <operation id="Blob.Attach">
        <param type="document" name="document">expr:Context["mailDocument"]</param>
        <param type="boolean" name="save">false</param>
        <param type="string" name="xpath">files:files</param>
      </operation>
    </chain>


## About using SSL with you mail server

If you connect to your mail server using SSL, your server needs to
have a valid certificate, that is, a certificate that is issued by a
known Authority. 

If this is not the case (you're using a self-made
certificate), then the JVM will refuse to connect and you'll have a
SSLHandShake error.

To be able to connect with a server with a self-signed certificate, you
need to add this certificate to the trusted certificate using keytool
with a command such as (see man keytool for more information):

    keytool -import -trustcacerts -file mail.cer -keystore thekeystore

If you don't have the certificate of the mail server you can get it
with the following command:

    openssl s_client -connect my.mailserver.com:PORT

You can either import the certificate in the cacerts of your JVM, or
create a new keystore and start the jvm with:

    -Djavax.net.ssl.trustStore=/home/foo/.keystore

Another option is to use an helper class from:
http://blogs.sun.com/andreas/entry/no_more_unable_to_find
and to follow this step:

1/ run the TestConnection class in eclipse as an application with your
   connection parameters

2/ if not working (error 'unable to find valid certification path to
   requested target') compile the java class:
   $ java InstallCert.java

3/ execute on imap server for instance:
   $ java InstallCert mail.example.com
   or
   $ java InstallCert mail.example.com:993

4/ make sure it is a good key before entering, and make sure it's been
   added to the default keystore in $JAVA_HOME/jre/lib/security/cacerts

5/ reproduce steps if needed, *do not forget to delete* the local file
   'jssecacerts' that's been created (otherwise the default keystore won't
   be updated anymore)

To test your setting you can use this class:
A TestConnection class sample

	public class TestImapConnection {

	    public static void main(String args[]) throws Exception {
		Properties props = new Properties();

		props.put("user", "email@example.com");
		props.put("password", "password");

		props.put("mail.store.protocol", "imap");
		props.put("mail.imap.host", "mail.example.com");
		props.put("mail.imap.port", "993");
		props.put("mail.imap.ssl.protocols", "SSL");
		props.put("mail.imap.starttls.enable", "true");

		props.put("mail.imap.socketFactory.class",
		        "javax.net.ssl.SSLSocketFactory");
		props.put("mail.imap.socketFactory.port", "993");
		props.put("mail.imap.socketFactory.fallback", "false");

		Session session = Session.getDefaultInstance(props);

		Store store = session.getStore();
		System.err.println("connecting");
		store.connect(props.getProperty("user").toString(), props.getProperty(
		        "password").toString());

		store.close();
		System.err.println("done");
	    }

	}

