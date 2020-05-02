# Redis Streams 101

This project use very simple scenarios to explain Redis Streams patterns.

The overall logic is the following:

* The producer generates messages and send 2 values "`number_1`" & "`number_2`"
* Then the application use various consumers (*services*) to process these numbers:
   * One service to do the **sum**
   * One service to do a the **division**
* This means most of the message will be properly processed, but in some cases the operation can fail (division by 0, not a number, ..). When it fails the consumer should keep the message in pending state and this should be cleanup up later on.


This document contains the following information:

1. Application Components: Description of all the services
2. Build and Run the Application
   * Build the Java Services
   * Build and Run the Docker project
   * Testing the application
   * Scaling the consumers
   * Testing the "Business Errors/Message Processing Failures"


---

## Application Components

This application has 5 services:

* `producer-numbers` : Java service that is used to post messages using REST to the `"app:calculator-events"` streams.
* `consumer-addition` : Java service that reads messages from the streams and do an addition with the 2 parameters stored in the message; and manage errors.
* `consumer-division` : Java service that reads messages from the streams and do a division with the 2 parameters stored in the message; and manage errors.
* `streams-info` : A small Node.JS to expose some of the Streams Information to the UI. (You can call http://localhost:3000/streams/all/app:calculator-events/ )
* `streams-web-client` : A Vue.js application to post messages and see that is happening in the various consumers.

### Producer "Numbers"

The `producer-numbers` directory contains a SpringBoot application that is used to post two numbers on a stream. (`"app:calculator-events"` by default).

This service is exposed using a simple REST Endpoint:

* http://localhost:8081/api/send-message?n1=10&n2=2


The method `RedisProducerService.postNewMessage` is called by the REST API and use Spring Data Redis to post a new message to the streams.

```
   Map<String,String> fields = new HashMap<>();
   fields.put("NUMBER_1", number1);
   fields.put("NUMBER_2", number2);
   StringRecord record = StreamRecords.string( fields  ).withStreamKey( "app:calculator-events" );
   template.opsForStream().add(record);
```

This will generate the following Redis Streams command:

```
XADD "app:calculator-events" * NUMBER_2 "2" NUMBER_1 "10"
```

Note: The Java part is not converting the parameter into long/int or any type of numerical value; and this is on purpose to be able to generate exception during the "consumer processing" (and keep the process simple for educational reason)


### Service "Addition" : `addition-service` Consumer Group

The `consumer-addition` directory contains also a SpringBoot application, with no endpoint. This is a pure processing service.

The Java class that does most of the work is: `io.redis.demo.streams.consumer.addition.StreamsConsumerAdditionService`.


The business logic, and "business exception" management has been implemented has followed, in the method `StreamsConsumerAdditionService.onMessage()`

1. The addition service, when created, will read "all existing messages" in the streams. So catching the history.
2. For each message receive the service will: (`XREADGROUP` using the SpringData `StreamListener` Class)
   * Convert the string variables into long
   * Do the sum of the two numbers
   * If **no-error** (the 2 parameters are numbers):
      * Send an acknowledgement (`XACK`) to the Consumer Group to indicates that the processing has been done and the message is **removed from the **pending** list**.
      * Update the `"app:service:addition"` Hash
   * If **error**, in this case probably a string to number conversion failure, that *simulates a failure in the processing of the message (or an incomoplete processing, for example a crash of the service)*, an exception is raised, and the **consumer does NOT send any acknolegment**. So the message is kept in the pending list. The management/processing of the failure in our case is not managed in this specific method, and will be done separately, this will allow for example the administrator, or any process to look at the source of the problem. (In our case it will be nice to track the application that generate the data and do some data management before)
      * The application also update the `"app:service:addition"` Hash by incrementing of errors. 


As mentioned above, this simple service, the Addition Service (the Division Service has an equivalent) creates a Hash `"app:service:addition"` that stores some results, the Hash looks like:

   * `result` : the total sum of all the calculations
   * `last_result` : the result of the last succesful calculation
   * `processed` : the number of successfully processed message.
   * `errors` : the number of messages that could not be processed due to an error. (mostly because one of the two parameters are not numbers)
   

**What is happening for the messages in error?**

In addition to the fact that you can scale and diversify the application with Consumer Group and Consumer (something that we will look in a minute), one of the key features of Redis Streams is that you can manage errors, and read again (or not) the failed messages.

So what happen to the message(s) that is not acknowledge by the consumer that is processing it?

* the message stays in the "pending list" that is associated to the consumer. If you are running the demonstration and generated some error go in Redis Insight or the CLI and run the following command: 
   * `XPENDING "app:calculator-events" "addition-service" - + 10 `
   * This lists the 10 messages that have not been processed successfully.
   * A pending message is made of the following property:
      * The ID of the message
      * the name of the consumer (in this application the consumer names are based on the IP address of the container/pod/machine)
      * The time of the latest delivery to a consumer
      * The number of time it has been delivered
* Once again the message stays in the pending list until "somebody" is doing an "`ACK`".
   * This somebody could be the same process/consumer or another one, but the first thing to do is to "`CLAIM`" the message.

Let's now explain how the "Calculator" application is dealing with the "Pending List".

The class `StreamsConsumerAdditionService` has been build as a scheduler that calls the method `StreamsConsumerAdditionService.processPendingMessages` every ten seconds (annotation in the code). In this application for simplicity reason, the process is made in the same class, but it could be a totally different process/application as soon as you know the Stream key (`"app:calculator-events"`)  and the Consumer Group name (`"addition-service"`).

How it works?

First of all, each of the service (Addition, Division) is configure to process or not the pending messages, Also it is done to only have one consumer doing it; this is not necessary, nor a good idea. It is done in this demonstration to be able to understand the overall flow of the messages when you work with Redis Streams.

So the `"app:retry:addition-service"` Has contains the following fields:

* `max_claimed_messages` : the number of messages that will be process out of the pending list for each "batch"
* `consumer_name` : the name of the consumer that will process the pending list. If none, the messages are not read... Allowing us to follow what is happening, and start/stop the pending process when needed.
* `max_retries` : Number of time the messages will be "claimed" and retry the processing before it will be acknowledged. (A logic could be to send message to another streams too, - issue #2)
* `delete_on_error` : if true, if and when all retries have failed, the process will delete the message from the stream (`XDEL'). In reallife you will probably copy the mesasage in another stream to manage error in a global place and still delete it like that if another process is reading the full streams it won't fail (we will play with this later.)


So the logic in the `StreamsConsumerAdditionService.processPendingMessages()` is the following:

* If the current consumer is the one that is configured to manage retry (`"app:retry:addition-service"`):
   * The system query the pending list and retrieve some messages (`XPENDING` using `RedisTemplate.opsForStream().pending(...)`).
   * For each of the messages, the process claim the message (`XCLAIN`), this will put the message in the consumer pending list, and increment the number of deliver in the pending list.
   * It is time to retrieve the message and try to reproduce it. This is done with the `XRANGE` command and use the message ID for both ends.
      * The business logic is the same as before, doing an addition with two number.
      * But this time we have added more logic, we use a call to an small utilise that convert string into numbers `WordsToNumberConverter.getNumberFromWords`. If the process update the map, and ACK the message. If not it fails again... *Note: we could have included this logic in the inital processing, the ida again is for learning process. We can imagine that the "live" processing has to be as fast and simple and possible, and in case of error the application will deal with more complex processing*.
   * So if the retry fails, we are back in square one :) - This sample application is at the end acknowledge the message after few failure.


To make it short:

* Message are read in one or many consumers in a Group using `XREADGROUP`
* Each message is processed:
   * When it is processed successfuly, the application send a `XACK`
   * If an error occurs before the message will stay in the Pending List.
* Then "another" time/process/part of the system is looking at the pending list with `XPENDING`
   * Then `XCLAIM` the message to process it
   * if success the process sends a `XACK`, if not the message stay in the Pending List.


### Service "Division" : `division-service` Consumer Group

Overall the logic is the same, the only part that are different:

* The process fails when the `NUMBER_2` is equals to zero (0).
* The process does not try to transform strings into number. (just by choice)
* This means that messages that fails in division will always fails. (once again for education purpose)


---

## Build and Run the Application

**Prerequisites**

* Git
* Java 8
* Apache Maven
* Docker

### Build the project


