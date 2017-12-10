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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.barracks.commons.test.PagedResourcesUtils;
import io.barracks.messagingservice.client.DeviceServiceClient;
import io.barracks.messagingservice.client.MQTTBrokerClient;
import io.barracks.messagingservice.client.exception.RabbitMQClientException;
import io.barracks.messagingservice.model.BarracksQuery;
import io.barracks.messagingservice.model.Device;
import io.barracks.messagingservice.model.Filter;
import io.barracks.messagingservice.model.User;
import io.barracks.messagingservice.utils.DeviceUtils;
import io.barracks.messagingservice.utils.FilterUtils;
import io.barracks.messagingservice.utils.UserUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.internal.verification.Times;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MessagingServiceManagerTest {

    @InjectMocks
    @Spy
    private MessagingServiceManager messagingServiceManager;

    @Mock
    private MQTTBrokerClient mqttBrokerClient;

    @Mock
    private DeviceServiceClient deviceServiceClient;

    @Test
    public void sendMessage_whenUnitIdAndFiltersSpecifiedAndSendMessageHasBeenSuccessful_shouldReturnAsyncResultWithTrue() throws InterruptedException, ExecutionException, TimeoutException {
        //Given
        final boolean clientSuccess = true;
        final User user = UserUtils.getUser();
        final String unitId1 = UUID.randomUUID().toString();
        final String unitId2 = UUID.randomUUID().toString();
        final String filter1 = UUID.randomUUID().toString();
        final String filter2 = UUID.randomUUID().toString();
        final List<String> unitIdList = Arrays.asList(unitId1, unitId2);
        final List<String> filtersList = Arrays.asList(filter1, filter2);
        final String message = "Coucou le device";
        final boolean retained = true;
        final long timeout = 1000L;

        doNothing().when(mqttBrokerClient).sendMessage(user.getApiKey(), unitId1, message, retained);
        doNothing().when(mqttBrokerClient).sendMessage(user.getApiKey(), unitId2, message, retained);
        doNothing().when(messagingServiceManager).sendMessageToDevicesInFilter(user, filter1, message, retained);
        doNothing().when(messagingServiceManager).sendMessageToDevicesInFilter(user, filter2, message, retained);

        //When
        final Future<Boolean> asyncResult = messagingServiceManager.sendMessage(user, unitIdList, filtersList, message, retained);

        //Then
        verify(mqttBrokerClient).sendMessage(user.getApiKey(), unitId1, message, retained);
        verify(mqttBrokerClient).sendMessage(user.getApiKey(), unitId1, message, retained);
        verify(messagingServiceManager).sendMessageToDevicesInFilter(user, filter1, message, retained);
        verify(messagingServiceManager).sendMessageToDevicesInFilter(user, filter1, message, retained);
        final Boolean result = asyncResult.get(timeout, TimeUnit.MILLISECONDS);
        assertThat(result).isEqualTo(clientSuccess);
    }

    @Test
    public void sendMessage_whenSendMessageHasFailed_shouldReturnAsyncResultWithFalse() throws InterruptedException, ExecutionException, TimeoutException {
        //Given
        final boolean clientFail = false;
        final User user = UserUtils.getUser();
        final String unitId1 = UUID.randomUUID().toString();
        final String unitId2 = UUID.randomUUID().toString();
        final String filter1 = UUID.randomUUID().toString();
        final String filter2 = UUID.randomUUID().toString();
        final List<String> unitIdList = Arrays.asList(unitId1, unitId2);
        final List<String> filtersList = Arrays.asList(filter1, filter2);
        final String message = "Coucou le device";
        final boolean retained = true;
        final long timeout = 1000L;
        doThrow(RabbitMQClientException.class).when(mqttBrokerClient).sendMessage(eq(user.getApiKey()), anyString(), eq(message), eq(retained));

        //When
        final Future<Boolean> asyncResult = messagingServiceManager.sendMessage(user, unitIdList, filtersList, message, retained);

        //Then
        verify(mqttBrokerClient).sendMessage(user.getApiKey(), unitId1, message, retained);
        final Boolean result = asyncResult.get(timeout, TimeUnit.MILLISECONDS);
        assertThat(result).isEqualTo(clientFail);
    }

    @Test
    public void sendMessage_whenSendMessageToFiltersHasFailed_shouldReturnAsyncResultWithFalse() throws InterruptedException, ExecutionException, TimeoutException {
        //Given
        final boolean clientFail = false;
        final User user = UserUtils.getUser();
        final String unitId1 = UUID.randomUUID().toString();
        final String unitId2 = UUID.randomUUID().toString();
        final String filter1 = UUID.randomUUID().toString();
        final String filter2 = UUID.randomUUID().toString();
        final List<String> unitIdList = Arrays.asList(unitId1, unitId2);
        final List<String> filtersList = Arrays.asList(filter1, filter2);
        final String message = "Coucou le device";
        final boolean retained = true;
        final long timeout = 1000L;
        doThrow(RabbitMQClientException.class).when(messagingServiceManager).sendMessageToDevicesInFilter(eq(user), anyString(), eq(message), eq(retained));

        //When
        final Future<Boolean> asyncResult = messagingServiceManager.sendMessage(user, unitIdList, filtersList, message, retained);

        //Then
        verify(mqttBrokerClient).sendMessage(user.getApiKey(), unitId1, message, retained);
        final Boolean result = asyncResult.get(timeout, TimeUnit.MILLISECONDS);
        assertThat(result).isEqualTo(clientFail);
    }

    @Test
    public void sendMessage_whenUnitIdButNoFiltersAndSendMessageHasBeenSuccessful_shouldReturnAsyncResultWithTrue() throws InterruptedException, ExecutionException, TimeoutException {
        //Given
        final boolean clientSuccess = true;
        final User user = UserUtils.getUser();
        final String unitId1 = UUID.randomUUID().toString();
        final String unitId2 = UUID.randomUUID().toString();
        final List<String> unitIdList = Arrays.asList(unitId1, unitId2);
        final List<String> filtersList = new ArrayList<>();
        final String message = "Coucou le device";
        final boolean retained = true;
        final long timeout = 1000L;

        doNothing().when(mqttBrokerClient).sendMessage(user.getApiKey(), unitId1, message, retained);
        doNothing().when(mqttBrokerClient).sendMessage(user.getApiKey(), unitId2, message, retained);

        //When
        final Future<Boolean> asyncResult = messagingServiceManager.sendMessage(user, unitIdList, filtersList, message, retained);

        //Then
        verify(mqttBrokerClient).sendMessage(user.getApiKey(), unitId1, message, retained);
        verify(mqttBrokerClient).sendMessage(user.getApiKey(), unitId2, message, retained);
        verify(messagingServiceManager, never()).sendMessageToDevicesInFilter(eq(user), anyString(), eq(message), eq(retained));
        final Boolean result = asyncResult.get(timeout, TimeUnit.MILLISECONDS);
        assertThat(result).isEqualTo(clientSuccess);
    }

    @Test
    public void sendMessage_whenFiltersButNoUnitIdAndSendMessageHasBeenSuccessful_shouldReturnAsyncResultWithTrue() throws InterruptedException, ExecutionException, TimeoutException {
        //Given
        final boolean clientSuccess = true;
        final User user = UserUtils.getUser();
        final String filter1 = UUID.randomUUID().toString();
        final String filter2 = UUID.randomUUID().toString();
        final List<String> filtersList = Arrays.asList(filter1, filter2);
        final List<String> unitIdList = new ArrayList<>();
        final String message = "Coucou le device";
        final boolean retained = true;
        final long timeout = 1000L;

        doNothing().when(messagingServiceManager).sendMessageToDevicesInFilter(user, filter1, message, retained);
        doNothing().when(messagingServiceManager).sendMessageToDevicesInFilter(user, filter2, message, retained);

        //When
        final Future<Boolean> asyncResult = messagingServiceManager.sendMessage(user, unitIdList, filtersList, message, retained);

        //Then
        verify(messagingServiceManager).sendMessageToDevicesInFilter(user, filter1, message, retained);
        verify(messagingServiceManager).sendMessageToDevicesInFilter(user, filter2, message, retained);
        verify(mqttBrokerClient, never()).sendMessage(eq(user.getApiKey()), anyString(), eq(message), eq(retained));
        final Boolean result = asyncResult.get(timeout, TimeUnit.MILLISECONDS);
        assertThat(result).isEqualTo(clientSuccess);
    }

    @Test
    public void sendMessageToAll_whenSendMessageHasBeenSuccessful_shouldReturnAsyncResultWithTrue() throws InterruptedException, ExecutionException, TimeoutException {
        //Given
        final boolean clientSuccess = true;
        final User user = UserUtils.getUser();
        final List<String> filtersList = new ArrayList<>();
        final List<String> unitIdList = new ArrayList<>();
        final String message = "Coucou le device";
        final boolean retained = true;
        final long timeout = 1000L;
        doNothing().when(messagingServiceManager).sendMessageToAllDevices(user, message, retained);

        //When
        final Future<Boolean> asyncResult = messagingServiceManager.sendMessage(user, unitIdList, filtersList, message, retained);

        //Then
        verify(messagingServiceManager).sendMessageToAllDevices(user, message, retained);
        final Boolean result = asyncResult.get(timeout, TimeUnit.MILLISECONDS);
        assertThat(result).isEqualTo(clientSuccess);
    }

    @Test
    public void sendMessageToAll_whenSendMessageHasFailed_shouldReturnAsyncResultWithFalse() throws InterruptedException, ExecutionException, TimeoutException {
        //Given
        final boolean clientFail = false;
        final User user = UserUtils.getUser();
        final List<String> filtersList = new ArrayList<>();
        final List<String> unitIdList = new ArrayList<>();
        final String message = "Coucou le device";
        final boolean retained = true;
        final long timeout = 1000L;
        doThrow(RabbitMQClientException.class).when(messagingServiceManager).sendMessageToAllDevices(user, message, retained);

        //When
        final Future<Boolean> asyncResult = messagingServiceManager.sendMessage(user, unitIdList, filtersList, message, retained);

        //Then
        verify(messagingServiceManager).sendMessageToAllDevices(user, message, retained);
        final Boolean result = asyncResult.get(timeout, TimeUnit.MILLISECONDS);
        assertThat(result).isEqualTo(clientFail);
    }

    @Test
    public void sendMessageToAllDevices_whenOnlyOnePage_shouldCallClient() {
        //Given
        final Pageable pageable = new PageRequest(0, 10);
        final User user = UserUtils.getUser();
        final String message = "salut";
        final boolean retained = true;
        final List<Device> deviceList = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            deviceList.add(DeviceUtils.getDevice());
        }
        final PagedResources<Device> pagedResources = PagedResourcesUtils.buildPagedResources(pageable, deviceList);
        doReturn(pagedResources).when(deviceServiceClient).getDevices(eq(user.getId()), any(Pageable.class), any(BarracksQuery.class));

        //When
        messagingServiceManager.sendMessageToAllDevices(user, message, retained);

        //Then
        verify(deviceServiceClient, new Times(1)).getDevices(eq(user.getId()), any(Pageable.class), any(BarracksQuery.class));
        verify(mqttBrokerClient, new Times(8)).sendMessage(eq(user.getApiKey()), anyString(), eq(message), eq(retained));
    }

    @Test
    public void sendMessageToAllDevices_whenMultiplePages_shouldCallClient() {
        //Given
        final Pageable pageable = new PageRequest(0, 10);
        final User user = UserUtils.getUser();
        final String message = "salut";
        final boolean retained = true;

        final List<Device> deviceList = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            deviceList.add(DeviceUtils.getDevice());
        }
        final PagedResources<Device> pagedResources = PagedResourcesUtils.buildPagedResources(pageable, deviceList);
        doReturn(pagedResources).when(deviceServiceClient).getDevices(eq(user.getId()), any(Pageable.class), any(BarracksQuery.class));

        //When
        messagingServiceManager.sendMessageToAllDevices(user, message, retained);

        //Then
        verify(deviceServiceClient, new Times(2)).getDevices(eq(user.getId()), any(Pageable.class), any(BarracksQuery.class));
        verify(mqttBrokerClient, new Times(30)).sendMessage(eq(user.getApiKey()), anyString(), eq(message), eq(retained));
    }

    @Test
    public void sendMessageToFilter_whenFilterExists_shouldCallClient() {
        //Given
        final Pageable pageable = new PageRequest(0, 10);
        final User user = UserUtils.getUser();
        final String message = "salut";
        final boolean retained = true;
        final String filterName = UUID.randomUUID().toString();

        final List<Device> deviceList = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            deviceList.add(DeviceUtils.getDevice());
        }

        final PagedResources<Device> pagedResources = PagedResourcesUtils.buildPagedResources(pageable, deviceList);
        final Filter filter = FilterUtils.getFilter();

        doReturn(filter).when(deviceServiceClient).getFilterByUserIdAndName(user.getId(), filterName);
        doReturn(pagedResources).when(deviceServiceClient).getDevices(eq(user.getId()), any(Pageable.class), any(BarracksQuery.class));

        //When
        messagingServiceManager.sendMessageToDevicesInFilter(user, filterName, message, retained);

        //Then
        verify(deviceServiceClient).getFilterByUserIdAndName(user.getId(), filterName);
        verify(deviceServiceClient, new Times(2)).getDevices(eq(user.getId()), any(Pageable.class), any(BarracksQuery.class));
        verify(mqttBrokerClient, new Times(30)).sendMessage(eq(user.getApiKey()), anyString(), eq(message), eq(retained));
    }

}
