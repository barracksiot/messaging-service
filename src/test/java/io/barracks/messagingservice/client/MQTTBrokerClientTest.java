/*
 * MIT License
 *
 * Copyright (c) 2017 Barracks Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.barracks.messagingservice.client;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@RestClientTest(MQTTBrokerClient.class)
public class MQTTBrokerClientTest {

    @Value("${io.barracks.mqtt.uri}")
    private String uri;

    @Autowired
    private MQTTBrokerClient mqttBrokerClient;

    @MockBean
    private MqttPahoClientFactory mqttPahoClientFactory;

    @Test
    public void sendMessage_whenAllIsFine_shouldPublishOnTopicAndDisconnect() throws MqttException {
        //Given
        final String apiKey = UUID.randomUUID().toString();
        final String unitId = UUID.randomUUID().toString();
        final String message = UUID.randomUUID().toString();
        final String topic = apiKey + "/" + unitId;
        final String clientId = apiKey + "." + unitId + ".sender";
        final boolean retained = false;
        final IMqttClient mqttClient = mock(MqttClient.class);
        final MqttMessage mqttMessage = new MqttMessage(message.getBytes(StandardCharsets.UTF_8));
        mqttMessage.setQos(1);
        mqttMessage.setRetained(retained);

        doReturn(mqttClient).when(mqttPahoClientFactory).getClientInstance(uri, clientId);

        //When
        mqttBrokerClient.sendMessage(apiKey, unitId, message, retained);

        //Then
        verify(mqttPahoClientFactory).getClientInstance(uri, clientId);
        verify(mqttClient).connect();
        verify(mqttClient).subscribe(topic);
        verify(mqttClient).publish(eq(topic), refEq(mqttMessage));
        verify(mqttClient).disconnect();
    }

}
