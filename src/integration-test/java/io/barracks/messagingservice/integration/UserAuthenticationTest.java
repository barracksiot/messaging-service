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

package io.barracks.messagingservice.integration;

import io.barracks.messagingservice.client.AuthorizationServiceClient;
import io.barracks.messagingservice.model.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserAuthenticationTest {
    @Value("${local.server.port}")
    private int port;
    private String serverUrl;

    // Template used for requesting
    @Autowired
    private TestRestTemplate testTemplate;

    @MockBean
    private AuthorizationServiceClient authorizationServiceClient;

    @Before
    public void setUp() {
        serverUrl = "http://localhost:" + port;
    }

    @Test
    public void request_shouldReturnForbidden_whenNotAuthenticated() {
        // Given
        doThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED)).when(authorizationServiceClient).requestUserFromToken(null);

        // When
        ResponseEntity responseEntity = testTemplate.exchange(
                serverUrl + "/whatever",
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                Void.class
        );

        // Then
        verify(authorizationServiceClient).requestUserFromToken(null);
        assertThat(responseEntity).hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN);
    }

    @Test
    public void request_shouldPerformNormally_whenAuthenticated() {
        // Given
        final String token = UUID.randomUUID().toString();
        final HttpHeaders headers = new HttpHeaders();
        headers.add("x-auth-token", token);
        final User user = User.builder().id(UUID.randomUUID().toString()).build();
        doReturn(user).when(authorizationServiceClient).requestUserFromToken(token);

        // When
        ResponseEntity responseEntity = testTemplate.exchange(
                serverUrl + "/whatever",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Void.class
        );
        // Then
        verify(authorizationServiceClient).requestUserFromToken(token);
        assertThat(responseEntity).hasFieldOrPropertyWithValue("statusCode", HttpStatus.NOT_FOUND);
    }
}
