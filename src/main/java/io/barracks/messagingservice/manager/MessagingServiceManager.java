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

package io.barracks.messagingservice.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.barracks.messagingservice.client.DeviceServiceClient;
import io.barracks.messagingservice.client.MQTTBrokerClient;
import io.barracks.messagingservice.client.exception.RabbitMQClientException;
import io.barracks.messagingservice.model.BarracksQuery;
import io.barracks.messagingservice.model.Device;
import io.barracks.messagingservice.model.Filter;
import io.barracks.messagingservice.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.PagedResources;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
public class MessagingServiceManager {

    private final MQTTBrokerClient mqttBrokerClient;

    private final DeviceServiceClient deviceServiceClient;

    @Autowired
    public MessagingServiceManager(MQTTBrokerClient mqttBrokerClient, DeviceServiceClient deviceServiceClient) {
        this.mqttBrokerClient = mqttBrokerClient;
        this.deviceServiceClient = deviceServiceClient;
    }

    @Async
    public Future<Boolean> sendMessage(User user, List<String> unitIdList, List<String> filtersList, String message, boolean retained) {
        boolean success;
        try {
            if (unitIdList.isEmpty() && filtersList.isEmpty()) {
                sendMessageToAllDevices(user, message, retained);
            } else {
                unitIdList.forEach(unitId -> mqttBrokerClient.sendMessage(user.getApiKey(), unitId, message, retained));
                filtersList.forEach(filterName -> sendMessageToDevicesInFilter(user, filterName, message, retained));
            }
            success = true;
        } catch (RabbitMQClientException e) {
            success = false;
        }
        return new AsyncResult<>(success);
    }

    void sendMessageToAllDevices(User user, String message, boolean retained) {
        final BarracksQuery query = new BarracksQuery(buildFirstSeenBeforeNowQuery());
        sendMessageToNextDevicesPages(user, message, retained, query, 0);
    }

    void sendMessageToDevicesInFilter(User user, String name, String message, boolean retained) {
        final Filter filter = deviceServiceClient.getFilterByUserIdAndName(user.getId(), name);
        final BarracksQuery query = new BarracksQuery(buildFirstSeenBeforeNowQuery(filter.getQuery()));
        sendMessageToNextDevicesPages(user, message, retained, query, 0);
    }

     private void sendMessageToNextDevicesPages(User user, String message, boolean retained, BarracksQuery query, int firstPageIndex) {
        final Pageable pageable = new PageRequest(firstPageIndex, 100, new Sort(Sort.Direction.ASC, "firstSeen"));
        final PagedResources<Device> page = deviceServiceClient.getDevices(user.getId(), pageable, query);
        page.getContent()
                .stream()
                .map(Device::getUnitId)
                .collect(Collectors.toList())
                .forEach(unitId -> mqttBrokerClient.sendMessage(user.getApiKey(), unitId, message, retained));
        final long totalPages = page.getMetadata().getTotalPages();
        if (firstPageIndex < totalPages-1) {
            sendMessageToNextDevicesPages(user, message, retained, query, ++firstPageIndex);
        }
    }

    private JsonNode buildFirstSeenBeforeNowQuery() {
        final String now = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT);
        final JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
        return jsonNodeFactory.objectNode()
                .set(
                        "lt",
                        jsonNodeFactory.objectNode()
                                .put("firstSeen", now)
                );
    }

    private JsonNode buildFirstSeenBeforeNowQuery(JsonNode jsonNode) {
        final JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
        final ArrayNode child = jsonNodeFactory.objectNode()
                .putArray("and")
                .add(jsonNode)
                .add(buildFirstSeenBeforeNowQuery());

        return jsonNodeFactory.objectNode()
                .set("and", child);
    }
}