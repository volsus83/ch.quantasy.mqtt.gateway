

# MQTT-Gateway
ch.quantasy.mqtt.gateway

This is a wrapper to [paho]'s [MQTT] library and allows to design data driven programs e.g. micro-services supporting the following general API:
<a href="https://github.com/knr1/ch.quantasy.mqtt.gateway/blob/master/TiMqWayService.svg">
<img src="https://github.com/knr1/ch.quantasy.mqtt.gateway/blob/master/TiMqWayService.svg.png" alt="Service-Diagram" />
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




[paho]: <https://github.com/eclipse/paho.mqtt.java>
[YAML]: <https://en.wikipedia.org/wiki/YAML>
[MQTT]: <http://mqtt.org/>
[TiMqWay.jar]: <https://prof.hti.bfh.ch/knr1/TiMqWay.jar>
[d3Viewer]: <https://github.com/hardillb/d3-MQTT-Topic-Tree>
[micro-service]: <https://en.wikipedia.org/wiki/Microservices>

