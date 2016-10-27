/*
 * /*
 *  *   "SeMqWay"
 *  *
 *  *    SeMqWay(tm): A gateway to provide an MQTT-View for any micro-service (Service MQTT-Gateway).
 *  *
 *  *    Copyright (c) 2016 Bern University of Applied Sciences (BFH),
 *  *    Research Institute for Security in the Information Society (RISIS), Wireless Communications & Secure Internet of Things (WiCom & SIoT),
 *  *    Quellgasse 21, CH-2501 Biel, Switzerland
 *  *
 *  *    Licensed under Dual License consisting of:
 *  *    1. GNU Affero General Public License (AGPL) v3
 *  *    and
 *  *    2. Commercial license
 *  *
 *  *
 *  *    1. This program is free software: you can redistribute it and/or modify
 *  *     it under the terms of the GNU Affero General Public License as published by
 *  *     the Free Software Foundation, either version 3 of the License, or
 *  *     (at your option) any later version.
 *  *
 *  *     This program is distributed in the hope that it will be useful,
 *  *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *     GNU Affero General Public License for more details.
 *  *
 *  *     You should have received a copy of the GNU Affero General Public License
 *  *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  *
 *  *
 *  *    2. Licensees holding valid commercial licenses for TiMqWay may use this file in
 *  *     accordance with the commercial license agreement provided with the
 *  *     Software or, alternatively, in accordance with the terms contained in
 *  *     a written agreement between you and Bern University of Applied Sciences (BFH),
 *  *     Research Institute for Security in the Information Society (RISIS), Wireless Communications & Secure Internet of Things (WiCom & SIoT),
 *  *     Quellgasse 21, CH-2501 Biel, Switzerland.
 *  *
 *  *
 *  *     For further information contact <e-mail: reto.koenig@bfh.ch>
 *  *
 *  *
 */
package ch.quantasy.mqtt.gateway.client;

import ch.quantasy.mqtt.communication.mqtt.MQTTCommunication;
import ch.quantasy.mqtt.communication.mqtt.MQTTCommunicationCallback;
import ch.quantasy.mqtt.communication.mqtt.MQTTParameters;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.net.URI;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;

/**
 *
 * @author reto
 * @param <S>
 */
public class GatewayClient<S extends ClientContract> implements MQTTCommunicationCallback {

    private Timer timer;

    private final MQTTParameters parameters;
    private final S contract;
    private final MQTTCommunication communication;
    private final ObjectMapper mapper;
    private final Map<String, Set<MessageConsumer>> messageConsumerMap;

    private final Map<String, Deque<MqttMessage>> intentMap;
    private final HashMap<String, MqttMessage> statusMap;
    private final HashMap<String, List<Object>> eventMap;
    private final HashMap<String, MqttMessage> contractDescriptionMap;

    /**
     * One executorService pool for all implemented Services within a JVM
     */
    private final static ExecutorService executorService;

    static {
        executorService = Executors.newCachedThreadPool();
    }

    public GatewayClient(URI mqttURI, String clientID, S contract) throws MqttException {
        this.contract = contract;
        messageConsumerMap = new HashMap<>();
        intentMap = new HashMap<>();

        statusMap = new HashMap<>();
        eventMap = new HashMap<>();
        contractDescriptionMap = new HashMap<>();
        mapper = new ObjectMapper(new YAMLFactory());
        mapper.setVisibility(VisibilityChecker.Std.defaultInstance().withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        mapper.configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true);
        communication = new MQTTCommunication();
        parameters = new MQTTParameters();
        parameters.setClientID(clientID);
        parameters.setIsCleanSession(false);
        parameters.setIsLastWillRetained(true);
        parameters.setLastWillMessage(contract.OFFLINE.getBytes());
        parameters.setLastWillQoS(1);
        parameters.setServerURIs(mqttURI);
        parameters.setWillTopic(contract.STATUS_CONNECTION);
        parameters.setMqttCallback(this);
        communication.connect(parameters);
        communication.publishActualWill(contract.ONLINE.getBytes());
        communication.subscribe(contract.INTENT + "/#", 1);

        addDescription(getContract().STATUS_CONNECTION, "[" + getContract().ONLINE + "|" + getContract().OFFLINE + "]");
    }

    public void connect() throws MqttException {
        if (communication.isConnected()) {
            return;
        }
        communication.connect(parameters);
        try {
            communication.publishActualWill(mapper.writeValueAsBytes(Boolean.TRUE));
            for (String subscription : messageConsumerMap.keySet()) {
                communication.subscribe(subscription, 1);
            }
        } catch (JsonProcessingException ex) {
            Logger.getLogger(GatewayClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void disconnect() throws MqttException {
        if (!communication.isConnected()) {
            return;
        }
        try {
            communication.publishActualWill(mapper.writeValueAsBytes(Boolean.FALSE));
            for (String subscription : messageConsumerMap.keySet()) {
                communication.unsubscribe(subscription);
            }
        } catch (JsonProcessingException ex) {
            Logger.getLogger(GatewayClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        communication.disconnect();
    }

    public synchronized void subscribe(String topic, MessageConsumer consumer) {
        if (!messageConsumerMap.containsKey(topic)) {
            messageConsumerMap.put(topic, new HashSet<>());
            communication.subscribe(topic, 1);
        }
        messageConsumerMap.get(topic).add(consumer);
    }

    public synchronized void unsubscribe(String topic, MessageConsumer consumer) {
        if (messageConsumerMap.containsKey(topic)) {
            Set<MessageConsumer> messageConsumers = messageConsumerMap.get(topic);
            messageConsumers.remove(consumer);
            if (messageConsumers.isEmpty()) {
                messageConsumerMap.remove(topic);
                communication.unsubscribe(topic);
            }
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken imdt) {
        //System.out.println("Delivery is done.");
    }

    public S getContract() {
        return contract;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    @Override
    public MqttMessage getMessageToPublish(String topic) {
        //For the status, only the latest one per topic is of interest.
        MqttMessage message = statusMap.get(topic);
        if (message != null) {
            return message;
        }
        //For the contract, only the latest one per topic is of interest.
        message = contractDescriptionMap.get(topic);
        if (message != null) {
            return message;
        }
        //For the event, all per topic are of interest. Hence a List of events is returned.
        List<Object> eventList = eventMap.get(topic);
        if (eventList != null) {
            eventMap.put(topic, new LinkedList<>());
            try {
                message = new MqttMessage(mapper.writeValueAsBytes(eventList));
                message.setQos(1);
                message.setRetained(true);
                return message;
            } catch (JsonProcessingException ex) {
                Logger.getLogger(GatewayClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        //For the intent, each of which per topic is of interest. Hence one after the other is called.
        Deque<MqttMessage> intents = intentMap.get(topic);
        if (intents != null) {
            MqttMessage intent = intents.pollFirst();
            if (!intents.isEmpty()) {
                communication.readyToPublish(this, topic);
            }
            return intent;
        }
        return null;
    }

    public void clearIntents(String topic) {
        intentMap.put(topic, null);
    }

    public void addIntent(String topic, Object intent) {
        try {
            MqttMessage message = null;
            if (intent != null) {
                message = new MqttMessage(mapper.writeValueAsBytes(intent));
            } else {
                message = new MqttMessage();
            }
            message.setQos(1);
            message.setRetained(true);
            topic = topic + "/" + contract.ID;
            Deque<MqttMessage> intents = intentMap.get(topic);
            if (intents == null) {
                intents = new LinkedList<>();
                intentMap.put(topic, intents);
            }
            intents.add(message);
            communication.readyToPublish(this, topic);

        } catch (JsonProcessingException ex) {
            Logger.getLogger(GatewayClient.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    protected void addEvent(String topic, Object event) {
        List<Object> eventList = eventMap.get(topic);
        if (eventList == null) {
            eventList = new LinkedList<>();
            eventMap.put(topic, eventList);
        }
        eventList.add(event);
        this.communication.readyToPublish(this, topic);
    }

    protected void addStatus(String topic, Object status) {
        try {
            MqttMessage message = null;
            if (status != null) {
                message = new MqttMessage(mapper.writeValueAsBytes(status));
            } else {
                message = new MqttMessage();
            }
            message.setQos(1);
            message.setRetained(true);
            statusMap.put(topic, message);
            communication.readyToPublish(this, topic);

        } catch (JsonProcessingException ex) {
            Logger.getLogger(GatewayClient.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    protected void addDescription(String topic, Object description) {
        try {
            MqttMessage message = new MqttMessage(mapper.writeValueAsBytes(description));
            message.setQos(1);
            message.setRetained(true);

            topic = topic.replaceFirst(getContract().ID_TOPIC, "");
            String descriptionTopic = getContract().DESCRIPTION + topic;
            contractDescriptionMap.put(descriptionTopic, message);
            communication.readyToPublish(this, descriptionTopic);

        } catch (JsonProcessingException ex) {
            Logger.getLogger(GatewayClient.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void connectionLost(Throwable thrwbl) {
        Logger.getLogger(GatewayClient.class
                .getName()).log(Level.SEVERE, null, thrwbl);
        System.out.println("Ouups, lost connection to subscriptions... will try again in some seconds");
        if (this.timer != null) {
            return;
        }
        this.timer = new Timer(true);
        this.timer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                try {
                    if (timer != null) {
                        communication.connect(parameters);
                        timer.cancel();
                        communication.publishActualWill(mapper.writeValueAsBytes(contract.ONLINE));
                        timer = null;
                    }

                } catch (Exception ex) {
                }
            }
        }, 0, 3000);
    }

    public static boolean compareTopic(final String actualTopic, final String subscribedTopic) {
        return actualTopic.matches(subscribedTopic.replaceAll("\\+", "[^/]+").replaceAll("#", ".+"));
    }

    @Override
    public void messageArrived(String topic, MqttMessage mm) {
        byte[] payload = mm.getPayload();
        if (payload == null) {
            return;
        }
        Set<MessageConsumer> messageConsumers = new HashSet<>();
        synchronized (this) {
            for (String subscribedTopic : this.messageConsumerMap.keySet()) {
                if (compareTopic(topic, subscribedTopic)) {
                    messageConsumers.addAll(this.messageConsumerMap.get(subscribedTopic));
                }
            }
        }
        //This way, even if a consumer has been subscribed itself under multiple topic-filters,
        //it is only called once per topic match.
        for (MessageConsumer consumer : messageConsumers) {
            executorService.submit(new Runnable() {
                @Override
                //Not so sure if this is a great idea... Check it!
                public void run() {
                    try {
                        consumer.messageArrived(GatewayClient.this, topic, payload);
                    } catch (Exception ex) {
                        Logger.getLogger(getClass().
                                getName()).log(Level.INFO, null, ex);
                    }
                }
            });

        }

    }
}