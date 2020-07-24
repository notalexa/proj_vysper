# Let Vysper and Jitsi meet

Extend Apache Vysper in order to implement the requirements of Jitsi.

### Motivation

Smart Homes should be able to organize audio/video conferences for the home members. Obviously, having a **keep your home private** policy in mind, this conference system needs to be hosted locally and I made a decision to use Jitsi for this purpose.
Additionally, we had the requirement to organize the attendees in a flexible way.  

A glance to the [manual installation guide of Jitsi](https://jitsi.github.io/handbook/docs/devops-guide/devops-guide-manual) shows, that the XMPP server is the central element of organization. Wanting Java as programming language, I searched for an XMPP server in Java. [Apache Vysper](https://mina.apache.org/vysper-project/) was the first choice because it seemed to be easily embeddable into other applications. Especially the integration of user management was interesting.

#### Example

To integrate cameras into your smart home, you may create an external component which organizes all cameras. Whenever a resident wants to know who is at the front door, he may enter a special room. At the time Jitsi opens this room, the XMPP server may automatically send an invite to the external component which establishes a video stream managed by Jitsi. In this way, you can
* Integrate all cameras.
* Automatically manage recording.
* May view the stream whereever Jitsi is supported.

### Changes

In order to make Vysper operable with Jitsi, I had to make some changes:
* Introduce the notation of "virtual hosts".
* Implement external components (XEP 0114).
* Extend the MUC implementation (especially handle IQ stanzas with namespace <code>http://jabber.org/protocol/muc#owner</code>).
* Fix some issues concerning relaying IQ stanzas and the BOSH implementation.
* In some cases, if a stanza is rewritten because it is forwarded, the answer needs to be rewritten either.

In order to achieve the first two requirements, a conceptual change interpreting `ServerRuntimeContext` was made. Instances implementing this interface represent either a domain or a component (or an external server) now.

The current code should not be considered as production ready. Some of the major defects are:
* The external component handshake doesn't check the password and doesn't check if the component has already logged in.
* The handling of http requests in the BOSH component needs to be reviewed.
* Some extensions of the MUC component (configuring the room for example) are not finished.
* Creating error stanzas should be checked, especially for stream creation.
* The logic for obtaining the handler of an answer of a rewritten stanza is a memory leak. No timeout is currently implemented.
* Server/server communication is currently not supported. The runtime context (and handling of connections) is lacking.

### Installation

To get a Vysper version usable with Jitsi, you should
1) Clone this project and import it into an eclipse workspace.
2) Download the latest release of Apache Vysper from [here (version 0.7)](https://mina.apache.org/vysper-project/download_0.7.html) and unzip it in the
cloned repository (in the root directory).
3) After a refresh, the project should compile correctly.
4) Modify the class `not.alexa.vysper.jitsi.Launcher`. Edit the static fields in there (the names are taken from the [manual installation guide of Jitsi](https://jitsi.github.io/handbook/docs/devops-guide/devops-guide-manual) mentioned above where explanations of the variables can be found).
5) Run the <code>update.sh</code> to create a new installation zip (use the Linux Subsystem for Windows on Windows for example).
6) Unzip the installation zip on your server.

Before starting, you need to generate keystores for the TLS keys and certificates. The server defines two domains. The first domain is the *meeting domain* (`jitsi.example.com`), the second one is used internally (`auth.jitsi.example.com`) for the *focus* user. Both needs a keystore (`jitsi.example.com.p12` resp. `auth.jitsi.example.com.p12`) in the `config` directory. To create it out of the private key and certificate mentioned in the manual installation guide, go to `/var/lib/prosody` and type

	cat jitsi.example.com.key jitsi.example.com.cer | openssl pkcs12 -export -in - -out jitsi.example.com.p12

resp.

	cat auth.jitsi.example.com.key auth.jitsi.example.com.cer | openssl pkcs12 -export -in - -out auth.jitsi.example.com.p12
(You need to enter the passwords as configured in the launcher class in this steps.)

Copy the output files into the `config` directory of the vysper installation.

Now you are ready to start the server:

	bin/jitsi-start-xmpp.sh jitsi.example.com
	
(*You should replace* `jitsi.example.com` *by the domain of your choice in the text above of course*.)
