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
import io.barracks.messagingservice.client.exception.AuthorizationServiceClientException;
import io.barracks.messagingservice.model.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.UUID;

import static io.barracks.messagingservice.client.AuthorizationServiceClient.TOKEN_TRANSLATION_ENDPOINT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(SpringRunner.class)
@RestClientTest(AuthorizationServiceClient.class)
public class AuthorizationServiceClientTest {

    @Autowired
    private MockRestServiceServer mockServer;

    @Autowired
    private AuthorizationServiceClient authorizationServiceClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${io.barracks.authorizationservice.base_url}")
    private String baseUrl;

    @Value("classpath:io/barracks/messagingservice/client/user.json")
    private Resource user;

    @Test
    public void getUserProfile_whenServiceFails_shouldThrowException() {
        // Given
        final String userToken = UUID.randomUUID().toString();
        mockServer.expect(method(HttpMethod.GET))
                .andExpect(requestTo(TOKEN_TRANSLATION_ENDPOINT.withBase(baseUrl).getURI()))
                .andExpect(header("X-Auth-Token", userToken))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        // When
        assertThatExceptionOfType(AuthorizationServiceClientException.class)
                .isThrownBy(() -> authorizationServiceClient.requestUserFromToken(userToken));

        // Then
        mockServer.verify();
    }

    @Test
    public void getUserProfile_whenServiceSucceeds_shouldReturnUserProfile() throws Exception {
        // Given
        final String userToken = UUID.randomUUID().toString();
        mockServer.expect(method(HttpMethod.GET))
                .andExpect(requestTo(TOKEN_TRANSLATION_ENDPOINT.withBase(baseUrl).getURI()))
                .andExpect(header("X-Auth-Token", userToken))
                .andRespond(withSuccess().body(user));

        // When
        final User result = authorizationServiceClient.requestUserFromToken(userToken);

        // Then
        mockServer.verify();
        assertThat(result).isEqualTo(objectMapper.readValue(user.getInputStream(), User.class));
    }

}