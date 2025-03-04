# Swiss QR API
[![Swiss QR CI](https://github.com/pontiussoftware/swissqr-api/actions/workflows/ci.yml/badge.svg)](https://github.com/pontiussoftware/swissqr-api/actions/workflows/ci.yml)
[![swagger-editor](https://img.shields.io/badge/open--API-in--editor-brightgreen.svg?style=flat&label=open-api-v3)](https://editor.swagger.io/?url=https://raw.githubusercontent.com/pontiussoftware/swissqr-api/master/docs/openapi-v3.json)

This is an Open API based RESTful service that generates and reads Swiss QR code payment slips. Currently, it offers the following functionality:

- Scan a Swiss QR Code payment slip from an image or a PDF and parse its content.
- Generate a Swiss QR code payment slip in PNG, PDF or SVG format.

The API is documented using the Open API standard, which - once the service is running - you find under ``/swagger-docs``. There is also a very simple UI under ``/swagger-ui``. 

## Building and running Swiss QR API
Swiss QR API runs on the JVM and was written in Kotlin. In order to build Swiss QR API you will require *Java JDK 21* or higher. The project comes with an integrated Gradle wrapper. Once you have checked out the source you can run it from within the project directory using ``./gradlew run`` or build it using ``./gradlew distTar`` or ``./gradlew distZip``.

Make sure you provide a path to a valid ``config.json`` file as program argument (see example file in project directory).

**Important:** The ``config.json`` can be used to configure the server port of the service (8081 by default). Furthermore, you can change / add API keys that can be used to interact with the service. It is recommended to change the default key for security reasons.

## Dockerfile

The project also comes with a Dockerfile, which can be used instead. You can build the Docker image from within the project directory using ```docker build .``` and run it using ```docker run -p 8081:8081 <image-id>```. Again, make sure you adjust the ``config.json`` file to your needs.

## Credits
Swiss QR API relies on a bunch of great open source-libraries. The ones I would like to mention here are:

- [SwissQRBill](https://github.com/manuelbl/SwissQRBill): A Java library to generate and decode Swiss QR bills.
- [Javalin](https://javalin.io/): A web server framework for Java and Kotlin.
- [BoofCV](https://boofcv.org/): An amazing computer vision framework for Java.

Thanks a lot to the great work of all people involved in these libraries (and of course all the others, that did not get a mention here)!