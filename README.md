# trep-websockets-beam-io
An Apache Beam source to connect and consume data from TREP using the Websocket API.

This project contains two components

1. An Apache Beam source TrepWsIO.
2. An example Google Dataflow pipeline that uses TrepWsIO to stream data to PubSub and/or BigQuery.

**Building the Trep-Websocket-IO project**

Note: Integration tests are dependent on specific TREP/ERT infrastructure & credentials and will report a failure until the tests are updated to fit your environment. To avoid this run the mvn commands with ``-DskipTests``

From the root directory run:

```
mvn clean
mvn install
```


To create a distributable ZIP file, `cd` to the `Dataflow-Pipeline` directory and run:

 ```
 mvn assembly:assembly
 ```



Alternatively run the `./build.sh` command from the root which contains all of the above commands in one script.

This is governed by the same Apache 2 open source license as defined in the LICENSE.md file.

# Contributing
In the event you would like to contribute to this repository, it is required that you read and sign the following:

- [Individual Contributor License Agreement](https://github.com/Refinitiv/trep-websockets-beam-io/blob/master/Individual%20Contributor%20License%20Agreement%20v1.docx)
- [Entity Contributor License Agreement](https://github.com/Refinitiv/trep-websockets-beam-io/blob/master/Entity%20Contributor%20License%20Agreement%20v1.1.docx)

Please email a signed and scanned copy to `sdkagreement@thomsonreuters.com`.  If you require that a signed agreement has to be physically mailed to us, please email the request for a mailing address and we will get back to you on where you can send the signed documents.

Documentation for the TREP WebSocket API (that this project is based on) and a Question & Answer forum are available at the  [WebSocket API Section of the Thomson Reuters Developer Community](https://developers.thomsonreuters.com/websocket-api).

# Support SLA
Issues raised via GitHub will be addressed in a best-effort manner. Please refer any questions and issues to me [Clive Stokes](mailto:clive.stokes@refinitiv.com)  in the first instance.
