

# MQTT-Gateway
ch.quantasy.mqtt.gateway

This is a wrapper to [paho]'s [MQTT] library and allows to design data driven programs e.g. micro-services supporting the following general API:
<a href="https://github.com/knr1/ch.quantasy.mqtt.gateway/blob/master/MqttGatewayClient.svg">
<img src="https://github.com/knr1/ch.quantasy.mqtt.gateway/blob/master/MqttGatewayClient.svg.png" alt="Interface-Diagram" />
</a>

## Ideology
Implementing the business-capabilites as micro-Services allows independent development and instanciation of the different aspects.
[martinFowler]

The different capabilities per micro-service are promoted in form of promises.
[promiseLinux],[promise] These promises are expressed by contract[contract], hence, each service provides a document based and independent API,
which is not bound to any programming language.[tolerant]


A message broker (publish subscirbe) is used to handle the flow of documents between the micro-services. The broker does not provide any domain specific business logic.

### Service Class vs. Service Instance
Every Service instance is a working unit (=U) of its abstract data type (class). Each unit has a distinct identifier <id>.


## API towards MQTT
The idea of this MQTT-Gateway is to provide some very generic but common API. There is nothing new, the following ideas are borrowed from different design ideologies. The idea
behind this API is to provide a simple and light-weight communication for the programs that provide mqtt access.

Per default, the implemented MQTT-Gateway expects [YAML] as data in- and output. However, this can be changed to any text- or binary or even hybrid solution.
### Unit
Each unit (=U) represents an instance of a Service-Class and uses an identifier <id> within its topic in order to be discriminated.

### Intent
The intent (=I) is the way, an MQTT-gateway-client should allow to be controlled / configured. The designer of the program (e.g. micro-service) defines the contracts on what data is accepted as
input from MQTT.
### Status
The status (=S) is the way, an MQTT-gateway-client should express its actual status. The designer of the program (e.g. micro-service) defines the contracts on how this
internal status is expressed towards MQTT.
### Event
The event (=E) is the way, an MQTT-gateway-client should express expected changes. The designer of the program (e.g. micro-service) defines the contracts on how this
change is expressed towards MQTT. It is important to notice, that events can occur in bursts. Hence, events are always delivered as arrays of events.
### Description
The description (=D) is the way, an MQTT-gateway-client should express the MQTT-API. This allows to read the possibilities for I, S and E by humans and by machines.

## API towards Java
### Construction
For construction of a GatewayClient. The mqttURI and the clientID is requested as parameters. 
Furthermore, a so called ClientContract is required. This contract defines the MQTT-Topics for I,S,E and D

### connect()
Only after calling the connect method, the connection will be established and the topic for the availability is set to online

### disconnect()
When this method is called, the topic for the availability is set to offline and the connection is closed.

### subscribe()
When the GatewayClient serves a service, the subscriptions should point to the according intent-topics.
Per subscription a MessageReceiver has to be provided as callback.

### publishIntent()
This method sends the message out as a single publish... i.e. if the network is slow, the publishes are queued.

This convenience method is used, in order to send some intent to a topic i.e. the intent-topic. As a rule of thumb:
* This method should never be used if the GatewayClient serves a service.
* This method should be used if the GatewayClient serves a servant i.e. in order to orchestrate services
* This method should be used if the GatewayClient serves an Agents i.e. in order to choreograph Servants

### publishStatus()
This method sends out the very latest message out as a single publish... i.e. if the network is slow, some publishes are lost.

This convenience method is used, in order to send some status to a topic i.e. the status-topic. As a rule of thumb:
This method should be used in order to provide status information (plural) about the internal program i.e. the service, the GatewayClient serves.

### publishEvent()
This method sends out the messages as an array of messages in a single publish. Dependent on the network speed, the message-array differs in size.

This convenience method is used, in order to send the events to a topic i.e. the event-topic. As a rule of thumb:
This method should be used in order to provide events that occur within the internal program i.e. the service, the GatewayClient serves.

### publishDescription()
This method sends out each message as a single publish... i.e. if the network is slow, the publishes are queued.

This convenience method is used, in order to send the description of the service abilities i.e. the contract(s). As a rule of thumb:
This method should be used in the very beginning only and should not change during life-time... It describes the abilities of the Service / Servant.


## Full Micro-Service
With the GatewayClient towards MQTT and the API towards the native programming language (Java), now the following generic composition can be used, in order to
provide micro-service capabilities to native programs, using a MVP (Model View Presenter) pattern, where the native program serves as 'model' (or source) and the MQTT side serves as 'view'. 
<a href="https://github.com/knr1/ch.quantasy.mqtt.gateway/blob/master/Micro-service.svg">
<img src="https://github.com/knr1/ch.quantasy.mqtt.gateway/blob/master/Micro-service.svg.png" alt="Micro-service-Diagram" />
</a>



[paho]: <https://github.com/eclipse/paho.mqtt.java>
[YAML]: <https://en.wikipedia.org/wiki/YAML>
[MQTT]: <http://mqtt.org/>
[TiMqWay.jar]: <https://prof.hti.bfh.ch/knr1/TiMqWay.jar>
[d3Viewer]: <https://github.com/hardillb/d3-MQTT-Topic-Tree>
[micro-service]: <https://en.wikipedia.org/wiki/Microservices>
[martinFowler]: <https://martinfowler.com/articles/microservices.html>
[promiseLinux]: <http://www.linuxjournal.com/content/promise-theory%E2%80%94what-it>
[promise]: <http://markburgess.org/BookOfPromises.pdf>
[contract]: <https://en.wikipedia.org/wiki/Design_by_contract>
[tolerant]: <https://martinfowler.com/bliki/TolerantReader.html>


