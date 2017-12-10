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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.barracks.commons.util.Endpoint;
import io.barracks.messagingservice.model.Filter;
import io.barracks.messagingservice.client.exception.DeviceServiceClientException;
import io.barracks.messagingservice.model.BarracksQuery;
import io.barracks.messagingservice.model.Device;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
public class DeviceServiceClient extends HateoasRestClient {

    static final Endpoint GET_DEVICES_ENDPOINT = Endpoint.from(HttpMethod.GET, "/owners/{userId}/devices", "query={query}");
    static final Endpoint GET_FILTER_ENDPOINT = Endpoint.from(HttpMethod.GET, "/owners/{userId}/filters/{name}");

    private final String baseUrl;

    private final RestTemplate restTemplate;

    @Autowired
    public DeviceServiceClient(
            ObjectMapper mapper,
            RestTemplateBuilder restTemplateBuilder,
            @Value("${io.barracks.deviceservice.v2.base_url}") String baseUrl
    ) {
        this.restTemplate = prepareRestTemplateBuilder(mapper, restTemplateBuilder).build();
        this.baseUrl = baseUrl;
    }

    public PagedResources<Device> getDevices(String userId, Pageable pageable, BarracksQuery query) {
        try {
            final ResponseEntity<PagedResources<Device>> responseEntity = restTemplate.exchange(
                    GET_DEVICES_ENDPOINT.withBase(baseUrl).pageable(pageable).getRequestEntity(userId, query.toJsonString()),
                    new ParameterizedTypeReference<PagedResources<Device>>() {
                    }
            );
            return responseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            throw new DeviceServiceClientException(e);
        }
    }

    public Filter getFilterByUserIdAndName(String userId, String name) {
        try {
            final ResponseEntity<Filter> responseEntity = restTemplate.exchange(
                    GET_FILTER_ENDPOINT.withBase(baseUrl).getRequestEntity(userId, name),
                    Filter.class
            );
            return responseEntity.getBody();
        }
        catch (HttpStatusCodeException e) {
            throw new DeviceServiceClientException(e);
        }

    }

}
