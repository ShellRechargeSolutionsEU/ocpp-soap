# Open Charge Point Protocol for Scala [![Build Status](https://secure.travis-ci.org/NewMotion/ocpp-soap.png)](http://travis-ci.org/NewMotion/ocpp-soap) [![Coverage Status](https://coveralls.io/repos/github/NewMotion/ocpp-soap/badge.svg?branch=master)](https://coveralls.io/github/NewMotion/ocpp-soap?branch=master)

The Open Charge Point Protocol (OCPP) is a network protocol for communication
between electric vehicle chargers and a central backoffice system. It is
developed by the Open Charge Alliance (OCA). You can find more details on the
[official website of the OCA](http://openchargealliance.org/).

This library provides support for OCPP versions 1.2 and 1.5 using SOAP as the
underlying RPC mechanism, using [NewMotion's base OCPP library](https://github.com/NewMotion/ocpp)
for the representation of OCPP messages.

## How to use

### Setup

The library is divided four separate modules so applications using it
won't get too many dependencies dragged in. Those are:

  * `ocpp-soap`: A version-independent interface to OCPP-SOAP
  * `ocpp-spray`: A library to help handling OCPP SOAP messages with Akka and
                  the Spray HTTP library
  * `ocpp-12` and `ocpp-15`: WSDL files and generated code for the OCPP 1.2 and
                             1.5 SOAP services

So if you want to use the high-level Spray-based interface, and you're
using SBT, you can declare the dependency by adding this to your `plugins.sbt`:

```
resolvers += "TNM" at "http://nexus.thenewmotion.com/content/groups/public"
```

and this to your `build.sbt`:

```
libraryDependencies += "com.thenewmotion" %% "ocpp-spray" % "8.0.2"
```

With Maven, you'd set up the repository in your pom.xml:
```xml
    <repository>
        <id>thenewmotion</id>
        <name>The New Motion Repository</name>
        <url>http://nexus.thenewmotion.com/content/repositories/releases-public</url>
    </repository>
```

and add this to your dependencies:

```xml
    <dependency>
        <groupId>com.thenewmotion.ocpp</groupId>
        <artifactId>ocpp-spray_2.11</artifactId>
        <version>8.0.2</version>
    </dependency>
```

## Changelog

### Changes in 8.1.0

- Drop Scala 2.11 support

### Changes in 8.0.3

- Add Scala 2.12 support

### Changes in 8.0.2

- Update dependencies

### Changes in 8.0.0

 - The SOAP-related subprojects are split off from the original `ocpp` project

## Licensing and acknowledgements

The contents of this repository are © 2012 - 2019 The New Motion B.V., licensed under the [GPL version 3](LICENSE), except the OCPP specification PDFs and WSDL files, which are licensed by the Open Charge Alliance as indicated in the files.

