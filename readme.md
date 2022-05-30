## Why Facket
### Why name it facket
Because my nickname is fan87, starts with `f`, and I'm planning to make a packet system, let's replace the first character
with `f`, boom, facket.

### Why using Facket
I don't know, because I'm a cool kid, and you should be using things made by a cool kid, lol.

I will also be using this library in my future projects (So I don't need to make the same packet system everywhere).
You can do things in an easy way, why do it in the hard way, right? That's why I'm and will be using this library.

Here's the features list anyway if you want to flex:
1. **Function based,** so you can just call a function, and boom, the data has been sent to another side, be processed, and
get the return value from it.
2. **Object-oriented**, so you can do this to your Player class or Client class or whatever it is, pass facket as constructor
parameter, and boom, you can now set health data by calling a method without actually sending a packet, isn't it awesome?
3. **Request based** (Optional), so if you want to send a request to the server, you won't need to worry about some crazy stuff like
request id anymore.
4. **Serialization based**, so you can send data of a mob without manually writing the code of how to serialize the mob
object. Well, if you really want to, you can still write a custom serializer, black/white list properties and more.
5. **No more (insanely) worrying about obfuscation**, since packets are in binary, no property name will be sent from
the client or server, they will be placed to the byte buffer in order.
6. **Minimized**, packet data are as minimized as possible


## Getting Started

### Keywords
Some keywords and what they mean, so you won't get lost while reading this section.

1. `Communication Class` - `A` will be calling it, and facket will send parameter data to `B`, and return the value
gotten from `B`
2. `Implementation communication class` -  After sending parameter data to `B`, `B` will have to handle it, and it's
through implementation communication class. It shouldn't be called by your program, but by facket.

### Warning
1. You can't have duplicated method name in Communication Class even with different parameters type
2. Communication class must have empty constructor or a supplier that returns the instance of implementation of 
communication class object

### Thread Safe
Every implementation of communication class will be called in another thread

### Security Control
Security control contains every option about security

#### Release Mode
> ⚠️ Please only enable it when you are making a release of an application. Do NOT turn this off while making an addon
> or a plugin of an application, testing them, and developing.

> ⚠️ Not yet implemented, it does nothing for now, but you should be doing it anyway because it may be implemented in future
> release.

With the release mode enabled, it will be as fast as possible, and it will assume that everything is going right, and
your code is secure.

Opposite mode of release mode is debug mode (default). Debug mode does extra check to make sure there's nothing wrong
with your code, if you disable it, it won't check those anymore, and it may lead you to some corrupted packet,
unexpected/un-understandable error, and even security problems.

### Protocol Version
Major and Minor is basically the same. Both of them must be equal to establish a connection.

-----

## Full Document
> ⚠️ Not worth reading everything unless you want to learn more about it or be sure about something without testing it or reading
> the code. But please at least go through every chapter name so you can know what you can learn in this section.

### Asynchronization
You can only do asynchronization if it has no return value (void) for some pretty obvious reasons. A very simple workaround
would be having 2 void methods both on client and server communication class like this:
```java
// Will be called on client
public void getFromServer(int id) {
    this.execute(id);    
}

// Will be called on server
public void returnToClient(int id, Object value) {
    this.execute(id, value);
}
```

Implementation:
```java
// Will be run on server
public void getFromServer(int id) {
    serverImpl.returnToClient(id, "Hello, World!");
}

// Will be run on client
public void returnToClient(int id, Object value) {
    System.out.println("Got a message from server: " + value);
}
```

### Encryption
Encrypt is not implemented by default, but you can encrypt packet data by yourself.

```java
client.setConnectionHandler(new ConnectionHandler() {
   @Override
   public RawData onReceiveRawData(RawData data) {
           RawData returnValue = new RawData();
           returnValue.data = decryptPacket(key, data.data, data.length);
           returnValue.length = returnValue.data.length;
           return returnValue;
     }

   @Override
   public RawData onSendRawData(RawData data) {
           RawData returnValue = new RawData();
           returnValue.data = encryptPacket(key, data.data, data.length);
           returnValue.length = returnValue.data.length;
           return returnValue;
     }

});
```

Make the `key` null by default, and add handler: `onConnectionCreated`, and do SSL or something like that by calling
communication classes with ssl handshake methods. After getting the key, you set the `key` to the key to encrypt/decrypt it,
and check if key is null in `onReceiveRawData` and `onSendRawData`. 

In this way, you can have unencrypted SSL handshake and encrypted packet data.


### Version System
It's using [Semantic Versioning 2.0](https://semver.org/), and there's a built-in function in facket that checks if your
application is safe to use the version of facket. It's useful when you are making an addon or a plugin of other applications.

The major version X (X.y.z) won't be changed that much, but it's still safer to use

### Performance
1. Do not update Security Control frequently because there are some cached values. Consider creating another
instance if you want to have a different Security Control.

### Security
1. You must NOT serialize untrusted classes
2. Calling methods of non-implementation of communication classes with illegal packets won't work (Unless you bind your
communication class to non-implementation of communication class, then it's possible to have attacker RCE your server)

#### Buffer Overflow
Yes, you DO need to worry about buffer overflow. Please please please make sure the `read` and `write` function reads
the size of an object correctly, if it doesn't match then GG.

### Serialization Rules
1. You must have a constructor that takes no parameter
2. You can have properties annotated with `FacketSerializeBlacklist`, then it won't serialize that property (Blacklist)
3. You can have properties annotated with `FacketSerializeWhitelist`, then the class will only have properties annotated
with the same annotation to be serialized (Whitelist)
4. It will try and call methods of `CustomFacketSerialization` if the class implements it.
5. `registerCustomSerializer` will override the previous rule
6. Every system class (rt.jar) won't be serialized
7. After java 9, you (probably) need to be worrying about the module system (Probably, I don't know much about it because
I don't use it, but I do know about `illegal-access` option) since it will use `setAccessible` to set property


### Obfuscation
You don't really need to worry about your classes, methods, and fields are being renamed. But you **DO** need to worry
about these:

1. You can't do member shuffler - You can't change the order of fields unless you have custom serializer that puts
property into a buffer in order.
2. **IMPORTANT** You need to use the same method name and class name mapping for server and client for communication classes.

Renaming fields won't change anything in the packet, the packet reads and writes properties in order. For example,
the first 4 bytes can be the first int property, 4 ~ 8 bytes can be the second int property, so no matter how you
rename these, it would work the same.

### Multi-module / Separate client and server
If you want to release server and client separately, you probably want to do 3 modules: `client`, `server`, and `common`.
Here's what they should have:

#### Client
The client should be everything in `common` and all client bound implementation communication class.

#### Server
The server should be everything in `common` and all server bound implementation communication class.

#### Common
Common should contain all communication classes and all objects that you would like to serialize.

### Official Naming
There's an official way to name the classes and methods, so everyone will have the same naming method, and it will be
easier to read other people's code.

1. `C<Name>` - Server bound / sent from client packet wrapper class. It should look something like this:

```java
import me.fan87.facket.api.CommunicationClass;
import me.fan87.facket.api.FacketClient;
import me.fan87.facket.api.annotations.BoundTo;

@BoundTo(CExampleImpl.class)
public class CExample extends CommunicationClass {
   public CExample(FacketClient client) {
      super(client);
   }

   public CExample() {
      super();
   }

   public String exampleMethod(String parameterOne, int parameterTwo) {
      return (String) this.execute(parameterOne, parameterTwo);
   }

}
```

2. `S<Name>` - Client bound / sent from server packet wrapper class. It should look something like this:

```java
import me.fan87.facket.api.CommunicationClass;
import me.fan87.facket.api.FacketClient;
import me.fan87.facket.api.annotations.BoundTo;

@BoundTo(SExampleImpl.class)
public class SExample extends CommunicationClass {
    public SExample(FacketServer server) {
        super(server);
    }
    
    public String exampleMethod(String parameterOne, int parameterTwo) {
        return (String) this.execute(parameterOne, parameterTwo);
    }
    
}
```

3. `C<Name>Impl` - Server bound / sent from client packet implementation class. The server will be the one executes
them (Since they were sent from the client). It should look something like this:

```java
import me.fan87.facket.api.annotations.BoundTo;

public class CExampleImpl extends CExample {
    public CExampleImpl() { super(); }
    
    public String exampleMethod(String parameterOne, int parameterTwo) {
        return parameterOne + parameterTwo; // Implementation of it. The client will be receiving this return value.
    }
    
}
```

4. `S<Name>Impl` - Client bound / sent from server packet implementation class. The server will be the one executes
   them (Since they were sent from the client). It should look something like this:

```java
import me.fan87.facket.api.annotations.BoundTo;

public class SExampleImpl extends SExample {
    public SExampleImpl() { super(); }

    public String exampleMethod(String parameterOne, int parameterTwo) {
        return parameterOne + parameterTwo; // Implementation of it. The server will be receiving this return value.
    }

}
```

## Known Issues
(Empty)

## Todo / Missing Features
1. Kotlin MultiPlatform Support (So you can use facket to communicate between Java backend and KotlinJS frontend)
2. Peer to peer connection support



-----

## Packet Specification

> ⚠️ TL;DR, this is not designed to be read for people using this API. Unless you want to make an implementation of facket
> in other languages, it's not worth reading.

You can port facket to other languages like Javascript (Well, it's obviously harder because I tried it before) or C++.
Feel free to do it, because I'm not doing it now. Here's the facket packet specification, so you know how to read 
the packets correctly.
### Handshake
To perform the handshake, client must send the data before the server, so server will actually wait for data that's sent
from the client before sending any data back.

**Client -> Server** Magic Value
```java
// Unsigned
(byte) 0xfa, (byte) 0xcc, (byte) 0xe7, (byte) 0x01
// 0xfacce701, can be memorized as faccet / facket. The 01 in the end means that it's client to server
```

If it doesn't match, it means that it's not a facket server/client.

**Client -> Server** Client's Protocol Major Version in this following format
```java
new byte[] {
   (byte) (protocolVersionMajor >> 24),
   (byte) (protocolVersionMajor >> 16),
   (byte) (protocolVersionMajor >> 8),
   (byte) protocolVersionMajor
}
```

**Client -> Server** Client's Protocol Minor Version in this following format
```java
new byte[] {
   (byte) (protocolVersionMinor >> 24),
   (byte) (protocolVersionMinor >> 16),
   (byte) (protocolVersionMinor >> 8),
   (byte) protocolVersionMinor
}
```

**Server -> Client** Magic Value
```java
// Unsigned
(byte) 0xfa, (byte) 0xcc, (byte) 0xe7, (byte) 0x02
// 0xfacce701, can be memorized as faccet / facket. The 02 in the end means that it's server to client
```

If it doesn't match, it means that it's not a facket server/client.

**Server -> Client** Server's Protocol Major Version in this following format
```java
new byte[] {
   (byte) (protocolVersionMajor >> 24),
   (byte) (protocolVersionMajor >> 16),
   (byte) (protocolVersionMajor >> 8),
   (byte) protocolVersionMajor
}
```

**Server -> Client** Server's Protocol Minor Version in this following format
```java
new byte[] {
   (byte) (protocolVersionMinor >> 24),
   (byte) (protocolVersionMinor >> 16),
   (byte) (protocolVersionMinor >> 8),
   (byte) protocolVersionMinor
}
```

**Server -> Client** Handshake State
```sh
(Enum) (Signed)
  SUCCESS(0x01),
  UNSUPPORTED_PROTOCOL_VERSION(0x02),
  UNKNOWN(0x00);  # Throw Exception
```

If success:
```java
assert clientProtocolVersionMajor == serverProtocolVersionMajor;
assert clientProtocolVersionMinor == serverProtocolVersionMinor;
```
If any of the assertions failed, it means that it isn't a valid facket server.

### Serialization
#### Standard Types:
```kotlin
typealas s1 = byte;     // Signed
typealas s2 = short;    // Signed
typealas s4 = int;      // Signed
typealas s4c = char;    // Signed
typealas s4f = float;   // Signed
typealas s8 = long;     // Signed
typealas s8d = double;  // Signed
```

#### Types:
```
enum Type {
   s1 NULL      = -0x01;
   s1 BOOLEAN   = 0x00;
   s1 BYTE      = 0x01;
   s1 SHORT     = 0x02;
   s1 INTEGER   = 0x03;
   s1 CHAR      = 0x04;
   s1 FLOAT     = 0x05;
   s1 LONG      = 0x06;
   s1 DOUBLE    = 0x07;
   s1 STRING    = 0x08;
   s1 OBJECT    = {
      s1 data   = 0x09;
      string className;  // object.getClass().getName()
   };
}
```

#### Sub-Types:
To make it easier to be understood, here's the format of it:
```
@FixedSerializer(java.lang.String) // In this case, java.lang.String will be serialized/deseralized with this format
@InheritSerializer(java.lang.Object) // In this case, every class that extends java.lang.Object will be used to serialize/deserialize it 
// <T> is the genric type
name<T> {
   s4 attribute; // An attribute
}
```

Types:
```
any<T> {
   Type<T> type;
   T data;
}


@FixedSerializer(java.lang.String)
string {
   s4 byteArrayLength;     // utf8.length
   byte[] utf8;            // Raw utf8 array of the text. Can be signed or unsigned it doesn't matter
}

@InheritSerializer(java.lang.Object)
object {
   // Dynamically Serialized Data
}


array<T = {any | byte | short | int | char | float | long | double | string}> {
   s4 length;              // content.length
   Type type;              // content.getClass().getComponentType()
   T[] content;            // Content of the array. Can be nested
}
```


#### Default Serializers
```
@FixedSerializer(java.lang.Class)
class {
   string className;       // clazz.getName()
}

@FixedSerializer(java.util.List)
list<T extends any> {
   s4 length;              // content.length
   T[] content;            // Content of the list. Can be nested
}
```

### Calling And Handling Method Call
```
enum PacketType {
   s1 SEND        = 0x00;     // Send request
   s1 RETURN      = 0x01;     // Return value from receiver
   s1 EXCEPTION   = 0x02;     // If anything went wrong
}


packet {}
request extends packet {}

```


**Caller**: The side that requests calling the method<br>
**Receiver**: The side that receive the method call, and return the value back to Caller
<br><br>

Caller and returner can both be client or server, it's a 2 way packet system, so you don't need different code on client
or server for serialization.

**Caller -> Returner** Send a `request` object
```
requestbjects = returnResponse | exceptionResponse;

request extends packet {
   s4 packetSize;                         // packet.length, packet is processed through the user provided processor
   processed<requestbjects> packet;       // Processed request binary data
}

methodCallRequest {
   PacketType type = PacketType.SEND;     // Send packet
   s4 packetId;                           // Random int generated by the caller, used to identify the method call
   s4 classHashCode;                      // String#hashcode() of the class name that the caller want to call (Not as same as method.getDeclaringClass()). The class name should be the communication class name, not the implementation of communication class name.
   s4 methodHashCode;                     // (method.getDeclaringClass().getName() + method.getName()).hashcode()
   any[] parameters;                      // Parameters
}
```
Caller should be implemented to expect a `response` object (See above) with same packet ID as `methodCallRequest.packetId`.
After a specific time `timeout`, it will throw an exception that tells the user or developer that the request has timed out.

Returner should be implemented to call the requested method, get the return value or exception that's thrown and send the
following packet

**Returner -> Caller** Handle a `methodCallRequest` by sending a `response` object
```
responseObjects = returnResponse | exceptionResponse;

response extends packet {
   s4 packetSize;                         // packet.length, packet is processed
   processed<responseObjects> packet;     // Processed response binary data
}


returnResponse {
   PacketType type = PacketType.RETURN;   // Return packet
   s4 packetId;                           // Must be same as methodCallRequest.packetid
   any returnValue;                       // The return value
}

exceptionResponse {
   PacketType type = PacketType.EXCEPTION;// Exception packet
   s4 packetId;                           // Must be same as methodCallRequest.packetid
   string exceptionClassName;             // e.getClass().getName(), assert e instanceof Throwable
   string message;                        // e.getMessage(), assert e instanceof Throwable
   string localizedMessage;               // e.getLocalizedMessage(), assert e instanceof Throwable
}
```

### Packet Processor provided by user
Every `packet.packet` (`response.packet` and `request.packet`) will go through the packet processor provided by the user
(`private ConnectionHandler connectionHandler` in `Facket.java`, `connectionHandler.onReceiveRawData` and
`connectionHandler.onSendRawData`, also known as connection handler).

If the user has modified the packet in connection handler / packet processor, the `responseObjects` wouldn't be the same
as the specification.
