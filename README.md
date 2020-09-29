# Swiss QR API
![Swiss QR CI](https://github.com/pontiussoftware/swissqr-api/workflows/Swiss%20QR%20CI/badge.svg?branch=master)
[![swagger-editor](https://img.shields.io/badge/open--API-in--editor-brightgreen.svg?style=flat&label=open-api-v3)](https://editor.swagger.io/?url=https://raw.githubusercontent.com/pontiussoftware/swissqr-api/master/docs/openapi-v3.json)

This is an Open API based RESTful service that generates and reads Swiss QR code payment slips. Currently, it offers the following functionality:

- Scan a Swiss QR Code payment slip from an image or a PDF and parse its content.
- Generate a Swiss QR code payment slip in PNG, PDF or SVG format.

The API is documented using the Open API standard, which -- once the service is running - you find under ``/swagger-docs``. There is also a very simple UI under ``/swagger-ui``. 

## Building and running Swiss QR API
Swiss QR API runs on the JVM and was written in Kotlin. In order to build Swiss QR API you will require *Java 11* or higher. The project comes with an integrated Gradle wrapper. Once you have checked out the source you can run it using ``./gradlew run`` or build it using ``./gradlew distTar`` or ``./gradlew distZip``.

Make sure you provide a path to a valid ``config.json`` file as program argument. Currently, you can only influence the port under which the Swiss QR API will run (8081 by default).

## Credits
Swiss QR API relies on a bunch of great open source- libraries. The ones I would like to mention here are:

- [SwissQRBill](https://github.com/manuelbl/SwissQRBill): An Java library to generate and decode Swiss QR bills .
- [Javalin](https://javalin.io/): An web server framework for Java and Kotlin.
- [BoofCV](https://boofcv.org/): A computer vision framework for Java

Thanks a lot to the great work of all people involved in these libraries (and of course all the others, that did not get a mention here)!