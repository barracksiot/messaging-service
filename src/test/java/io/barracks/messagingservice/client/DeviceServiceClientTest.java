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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.barracks.commons.util.Endpoint;
import io.barracks.messagingservice.client.exception.DeviceServiceClientException;
import io.barracks.messagingservice.model.BarracksQuery;
import io.barracks.messagingservice.model.Device;
import io.barracks.messagingservice.model.Filter;
import io.barracks.messagingservice.utils.BarracksQueryUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(SpringRunner.class)
@RestClientTest(DeviceServiceClient.class)
public class DeviceServiceClientTest {

    @Autowired
    private ObjectMapper mapper;
    @Value("${io.barracks.deviceservice.v2.base_url}")
    private String baseUrl;
    @Autowired
    private MockRestServiceServer mockServer;
    @Autowired
    private DeviceServiceClient deviceServiceClient;

    @Value("classpath:io/barracks/messagingservice/client/devices.json")
    private Resource devices;
    @Value("classpath:io/barracks/messagingservice/client/getFilter.json")
    private Resource getFilter;


    @Test
    public void getDevicesWithQuery_whenRequestSucceed_shouldReturnDevices() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.GET_DEVICES_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final BarracksQuery query = BarracksQueryUtils.getQuery();
        final JsonNode jsonObject = mapper.readTree(devices.getInputStream());
        final ArrayNode parsedDevices = ((ArrayNode) jsonObject.get("_embedded").get("devices"));
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).pageable(pageable).getURI(userId, query.toJsonString())))
                .andRespond(withSuccess().body(devices));

        // When
        final PagedResources<Device> result = deviceServiceClient.getDevices(userId, pageable, query);

        // Then
        mockServer.verify();
        assertThat(result.getContent())
                .isNotNull()
                .hasSize(parsedDevices.size());
    }

    @Test
    public void getDevicesWithQuery_whenRequestFailed_shouldThrowException() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.GET_DEVICES_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final Pageable pageable = new PageRequest(0, 10);
        final BarracksQuery query = BarracksQueryUtils.getQuery();
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).pageable(pageable).getURI(userId, query.toJsonString())))
                .andRespond(withServerError());

        // When / Then
        assertThatExceptionOfType(DeviceServiceClientException.class)
                .isThrownBy(() -> deviceServiceClient.getDevices(userId, pageable, query));
        mockServer.verify();
    }

    @Test
    public void getFilter_whenSucceeded_shouldReturnFilter() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.GET_FILTER_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String filterName = UUID.randomUUID().toString();
        final Filter expected = mapper.readValue(this.getFilter.getInputStream(), Filter.class);
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, filterName)))
                .andRespond(withSuccess().body(getFilter));

        // When
        final Filter result = deviceServiceClient.getFilterByUserIdAndName(userId, filterName);

        // Then
        mockServer.verify();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getFilter_whenFails_shouldThrowException() throws Exception {
        // Given
        final Endpoint endpoint = DeviceServiceClient.GET_FILTER_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String filterName = UUID.randomUUID().toString();

        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, filterName)))
                .andRespond(withBadRequest());

        // ThenWhen
        assertThatExceptionOfType(DeviceServiceClientException.class)
                .isThrownBy(() -> deviceServiceClient.getFilterByUserIdAndName(userId, filterName));
        mockServer.verify();
    }

}
