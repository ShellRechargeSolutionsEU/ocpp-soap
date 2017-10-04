# Open Charge Point Protocol for Scala [![Build Status](https://secure.travis-ci.org/NewMotion/ocpp.png)](http://travis-ci.org/NewMotion/ocpp)

The Open Charge Point Protocol (OCPP) is a network protocol for communication
between electric vehicle chargers and a central backoffice system. It is
developed by the Open Charge Alliance (OCA). You can find more details on the
[official website of the OCA](http://openchargealliance.org/).

This library is the implementation of OCPP developed and used by NewMotion, one
of Europe's largest Electric Vehicle Charge Point Operators.

The library is designed with versatility in mind. OCPP comes in 3 versions (1.2,
1.5 and 1.6), two transport variants (SOAP/XML aka OCPP-S and WebSocket/JSON aka
OCPP-J), and two roles ("Charge Point" and "Central System"). This library will
help you with almost any combination of those. Only version 1.2 with
WebSocket/JSON and version 1.6 with SOAP/XML are not possible.

Also, you will probably want to use different WebSocket libraries for
different scenarios: a production back-office server with tens of thousands of
concurrent connections, a client in a load testing tool, or a simple one-off
script to test a certain behavior. This library uses the
[cake pattern](http://www.cakesolutions.net/teamblogs/2011/12/19/cake-pattern-in-depth)
to make it easy to swap out the underlying WebSocket implementation while still
using the same concise high-level API.

## How to use

### Setup

The library is divided into a bunch of separate modules so applications using it
won't get too many dependencies dragged in. Those are:

  * `ocpp-j-api`: high-level interface to OCPP-J connections
  * `ocpp-json`: serialization of OCPP messages to/from JSON
  * `ocpp-messages`: A model of OCPP messaging that is independent of protocol
                     version and transport variant

  * `ocpp-soap`: A version-independent interface to OCPP-SOAP
  * `ocpp-spray`: A library to help handling OCPP SOAP messages with Akka and
                  the Spray HTTP library
  * `ocpp-12` and `ocpp-15`: WSDL files and generated code for the OCPP 1.2 and
                             1.5 SOAP services

So if you want to use the high-level OCPP-J connection interface, and you're
using SBT, you can declare the dependency by adding this to your `plugins.sbt`:

```
resolvers += "TNM" at "http://nexus.thenewmotion.com/content/groups/public"

```

and this to your `build.sbt`:

```
libraryDependencies += "com.thenewmotion" %% "ocpp-j-api" % "6.0-beta1"
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
        <artifactId>ocpp-j-api_2.11</artifactId>
        <version>6.0-beta1</version>
    </dependency>
```

### Using the simple client API

An example OCPP-J client application included. You can run it like this:

    sbt "project example-json-client" "run 01234567 ws://localhost:8017/ocppws 1.5"

This means: connect to the Central System running at
`ws://localhost:8017/ocppws`, as a charge point with ID 01234567, using OCPP
version 1.5. Leave out the "1.5", or type "1.6", to use OCPP version 1.6
instead.

If you look at the code of the example by clicking [here](example-json-client/src/main/scala/com/thenewmotion/ocpp/json/example/JsonClientTestApp.scala),
you can see how the client API is used:

 * A connection is established by creating an instance of `OcppJsonClient`. The
   server endpoint URI, charge point ID and OCPP version to use are passed
   to the constructor.

 * To specify how your application handles incoming messages, you override the
   `requestHandler` and `onError` members.

 * To send OCPP messages to the Central System, you call the `send` method on
   the `OcppJsonClient` instance. You will get a `Future` back that will be
   completed with the Central System's response. If the Central System fails
   to respond to your request, the `Future` will fail.

 * `OcppJsonClient` is an instance of the [`OcppEndpoint`](ocpp-j-api/src/main/scala/com/thenewmotion/ocpp/json/api/OcppEndpoint.scala)
   trait. This trait defines this interface.

#### Handling requests

To specify the request handler, we use a [_magnet pattern_](http://spray.io/blog/2012-12-13-the-magnet-pattern/).
You can specify the request handler in different ways. After the
`val requestHandler: ChargePointRequestHandler =`, you see a
[`ChargePoint`](ocpp-messages/src/main/scala/com/thenewmotion/ocpp/messages/ChargePoint.scala)
instance in the example program. But you can also specify the request handler
as a function from `ChargePointReq` to `Future[ChargePointRes]`:

```scala
    val requestHandler: ChargePointRequestHandler = { (req: ChargePointReq) =>
      req match {
        case GetConfigurationReq(keys) =>
          System.out.println(s"Received GetConfiguration for $keys")
          Future.successful(GetConfigurationRes(
            values = List(),
            unknownKeys = keys
          ))
        case x =>
          val opName = x.getClass.getSimpleName
          Future.failed(OcppException(
            PayloadErrorCode.NotSupported,
            s"Demo app doesn't support $opName"
          ))
      }
    }
```

This behavior of this request handler is more or less equivalent to that of the
one in the example app. It is shorter at the price of being less type-safe:
this code does not check if you generate the right response type for the
request, so if you generate a GetConfigurationRes in response to a
GetConfigurationReq for instance.

#### Sending requests

Sending requests is simple, as explained. You call the `send` method of your
endpoint and off you go, like this:

```scala
    connection.send(HeartbeatReq)
```

The set of messages you can send is defined in [ocpp-messages](ocpp-messages/src/main/scala/com/thenewmotion/ocpp/messages/Message.scala).
For every request type, you represent requests as instances of a case class
named `<Operation Name>Req`, e.g. `StatusNotificationReq`, `HeartbeatReq`.

These case classes in `ocpp-messages` are designed according to two principles:
 * They are independent of OCPP version, so you have one interface to charging
   stations that use different versions
 * They sometimes group and rearrange fields to make it impossible to specify
   nonsense messages (e.g., no `vendorErrorCode` in status notifications that
   are not about errors). This makes it easier to write the code dealing with
   those requests, which does not have to validate things first.

This does mean that sometimes the way these case classes are defined may be a
bit surprising to people familiar with the OCPP specification. It be so. Use
the link to the file above, or use ⌘P in IntelliJ IDEA, to see how to give these
case classes the right parameters to formulate the request you want to send.

This also means that it is possible to send requests that cannot be represented
in the OCPP version that is used for the connection you send them over. In that
case `send` will return a failed future with an `OcppError` with error code
`NotSupported`.

The result of the `send` method is a `Future[RES]`, where `RES` is the type
of the response that belongs to the request you sent. So the type of this
expression:

```scala
     connection.send(AuthorizeReq(idTag = "12345678"))
```

is `Future[AuthorizeRes]`.

And if you want to do something with the result, the code could look like this:

```scala
     connection.send(AuthorizeReq(idTag = "12345678")).map { res =>
       if (res.idTag.status == AuthorizationStatus.Accepted)
         System.out.println("12345678 is authorized.")
       else
         System.out.println("12345678 has been rejected. No power to you!")
     }
```

Note that the library does not by itself
enforce the OCPP requirement that you wait for the response before sending the
next request. A simple way to obey it is chaining the send operations in a `for`
comprehension, as shown in the example app.

### Rolling your own `OcppEndpoint`

TODO

### Just serializing

If you do not need the connection management provided by the high-level API,
you can still use the `ocpp-json` module for serializing and deserializing OCPP
messages that you will send or receive using other libraries.

To do so, call the methods in the [OcppJ](ocpp-json/src/main/scala/com/thenewmotion/ocpp/json/OcppJ.scala)
object after importing either
`com.thenewmotion.ocpp.json.v15.SerializationV15._`
or `com.thenewmotion.ocpp.json.v16.SerializationV16._` to select which OCPP
version to use:

```scala

    import com.thenewmotion.ocpp.json.OcppJ
    import com.thenewmotion.ocpp.messages
    import com.thenewmotion.ocpp.Version
    import com.thenewmotion.ocpp.json.v16.SerializationV16._

    OcppJ.write(AuthorizeReq(idTag = "ABCDEF012"))
    // this results in:
    // res6: String = {"idTag":"ABCDEF012"}

    OcppJ.read[AuthorizeReq, Version.V16.type]("""{"idTag":"ABCDEF012"}""")
    // this results in:
    // res10: com.thenewmotion.ocpp.messages.AuthorizeReq = AuthorizeReq(ABCDEF012)
```

There are also `serialize` and `deserialize` methods on the `OcppJ` object that
use json4s `JValue`s as the representation of JSON instead of raw `String`s.
You can use those to build the [SRPC](http://www.gir.fr/ocppjs/ocpp_srpc_spec.shtml)
messages that are sent over the WebSocket. See [TransportMessage](ocpp-json/src/main/scala/com/thenewmotion/ocpp/json/TransportMessageProtocol.scala)
and [TransportMessageJsonSerializers](ocpp-json/src/main/scala/com/thenewmotion/ocpp/json/TransportMessageJsonSerializers.scala)
for how to work with those.

### SOAP

OCPP-S is what this library started with, but by now it is undocumented legacy.
It may be split off to enjoy retirement in its own little project on the next
major version.

### Changes compared to version 4.x

This library had been stable for a few years between 2014 and 2017, with 4.x.x
version numbers, supporting OCPP-S 1.2 and 1.5, and OCPP-J 1.5, but not 1.6. Now
that 1.6 support has been added, many wildly incompatible changes to the library
interface were made while we were at it. The most important ones to be aware of
when porting older code:

 - The `CentralSystem` and `ChargePoint` traits were renamed to
   `SyncCentralSystem` and `SyncChargePoint`. The names `CentralSystem` and
   `ChargePoint` are now used for asynchronous versions of these traits that
   return `Future`s.
 - In the high-level JSON API, request-response-handling has become more
   type-safe. Your request handler is no longer just a function from requests
   to responses, but now a `RequestHandler` which will also verify that you
   produce the right response type for the given request.
 - The library now uses [enum-utils](https://github.com/NewMotion/enum-utils)
   instead of Scala's `Enumeration`s
 - The library now uses Java 8's `java.time` for date and time handling instead
   of `com.thenewmotion.time`.
 - `JsonDeserializable` was renamed to `JsonOperation` and now handles not only
   deserialization but also serialization of OCPP messages for OCPP-J.
 - `OcppJsonClient` now takes a version parameter

## TODO

 - OCPP 1.5/1.6 version handshaking in the OCPP JSON client

 - Building for Scala 2.12. May require dropping the SOAP and/or switching JSON
   libraries.

 - Explaining how to build a server endpoint interface under "roll your own
   ednpoint" above

## Acknowledgements

Most of the example messages in the ocpp-json unit tests were taken from
[GIR ocppjs](http://www.gir.fr/ocppjs/).
