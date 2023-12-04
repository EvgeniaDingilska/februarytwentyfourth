WIREMOCK setup for Milestone Mobile Server (standalone)

=======================================================
WARNING: Under development. Everything can change.
=======================================================

Getting started:
    * To start WireMock as standalone server:
      > java -jar wiremock.jar --port=8081 --https-port=8082
      > java -jar wiremock-jre8-standalone-2.35.0.jar --port=8081 --https-port=8082
	  
	*Start with Cors
	  > java -jar wiremock.jar --port=8081 --https-port=8082 --enable-stub-cors 
      > java -jar wiremock-jre8-standalone-2.35.0.jar --port=8081 --https-port=8082 --enable-stub-cors 
	  
    * You can then add it to your mobile client and connect to it as usual
      (use your PC's address). User name and password can be arbitrary.

Changing operation:
    * To turn on two-factor authentication: increase the priority of login_error command ("priority":1)
    * To turn on in-app notifications: decrease the priority of livemessage command ("priority":10)
    * [!] Make sure to restart the mock server after you save your changes

Extending it:
    * To add stubs for new commands, add JSON files in the mappings folder 
      (you can use one of the existing files as a template)
    * The command response can be either inlined in the JSON or separate XML file in 
      the __files folder. 
      [!] Make sure the file is saved in DOS mode (CRLF) and that it ends with two
      empty lines (/r/n/r/n if inlined in JSON or two empty lines in the XML)

Limitations:
    * Video is partially supported, and in Pull Mode only
    * User name/password are not validated
    * Playback commands are not supported
    * Not all MoS features are mocked (yet). Work in progress.
    * ...