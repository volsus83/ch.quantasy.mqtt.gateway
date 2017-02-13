

# MQTT-Gateway
ch.quantasy.mqtt.gateway

This is a wrapper to [paho]'s [MQTT] library and allows to design data driven programs e.g. micro-services supporting the following general API:
<a href="https://github.com/knr1/ch.quantasy.mqtt.gateway/blob/master/MqttGatewayClient.svg">
<img src="https://github.com/knr1/ch.quantasy.mqtt.gateway/blob/master/MqttGatewayClient.svg.png" alt="Interface-Diagram" />
</a>
##API towards MQTT
The idea of this MQTT-Gateway is to provide some very generic but common API. There is nothing new, the following ideas are borrowed from different design ideologies. The idea
behind this API is to provide a simple and light-weight communication for the programs that provide mqtt access.

Per default, the implemented MQTT-Gateway expects [YAML] as data in- and output. However, this can be changed to any text- or binary or even hybrid solution.
###Intent
The intent (=I) is the way, an MQTT-gateway-client should allow to be controlled / configured. The designer of the program (e.g. micro-service) defines the contracts on what data is accepted as
input from MQTT.
###Status
The status (=S) is the way, an MQTT-gateway-client should express its actual status. The designer of the program (e.g. micro-service) defines the contracts on how this
internal status is expressed towards MQTT.
###Event
The event (=E) is the way, an MQTT-gateway-client should express expected changes. The designer of the program (e.g. micro-service) defines the contracts on how this
change is expressed towards MQTT. It is important to notice, that events can occur in bursts. Hence, events are always delivered as arrays of events.
###Description
The description (=D) is the way, an MQTT-gateway-client should express the MQTT-API. This allows to read the possibilities for I, S and E by humans and by machines.

##API towards Java
###Construction
For construction of a GatewayClient. The mqttURI and the clientID is requested as parameters. 
Furthermore, a so called ClientContract is required. This contract defines the MQTT-Topics for I,S,E and D

###connect
Only after calling the connect method, the connection will be established and the topic for the availability is set to online

###disconnect
When this method is called, the topic for the availability is set to offline and the connection is closed.

###subscribe
When the GatewayClient serves a service, the subscriptions should point to the according intent-topics.
Per subscription a MessageReceiver has to be provided as callback.

###publishIntent
This method sends the message out as a single publish... i.e. if the network is slow, the publishes are queued.

This convenience method is used, in order to send some intent to a topic i.e. the intent-topic. As a rule of thumb:
* This method should never be used if the GatewayClient serves a service.
* This method should be used if the GatewayClient serves a servant i.e. in order to orchestrate services
* This method should be used if the GatewayClient serves an Agents i.e. in order to choreograph Servants

###publishStatus
This method sends out the very latest message out as a single publish... i.e. if the network is slow, some publishes are lost.

This convenience method is used, in order to send some status to a topic i.e. the status-topic. As a rule of thumb:
This method should be used in order to provide status information (plural) about the internal program i.e. the service, the GatewayClient serves.

###publishEvent
This method sends out the messages as an array of messages in a single publish. Dependent on the network speed, the message-array differs in size.

This convenience method is used, in order to send the events to a topic i.e. the event-topic. As a rule of thumb:
This method should be used in order to provide events that occur within the internal program i.e. the service, the GatewayClient serves.

###publishDescription
This method sends out each message as a single publish... i.e. if the network is slow, the publishes are queued.

This convenience method is used, in order to send the description of the service abilities i.e. the contract(s). As a rule of thumb:
This method should be used in the very beginning only and should not change during life-time... It describes the abilities of the Service / Servant.


[paho]: <https://github.com/eclipse/paho.mqtt.java>
[YAML]: <https://en.wikipedia.org/wiki/YAML>
[MQTT]: <http://mqtt.org/>
[TiMqWay.jar]: <https://prof.hti.bfh.ch/knr1/TiMqWay.jar>
[d3Viewer]: <https://github.com/hardillb/d3-MQTT-Topic-Tree>
[micro-service]: <https://en.wikipedia.org/wiki/Microservices>

