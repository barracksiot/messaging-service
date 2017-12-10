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

package io.barracks.messagingservice.rest.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.barracks.commons.util.Endpoint;
import io.barracks.messagingservice.rest.MessageResource;
import io.barracks.messagingservice.utils.RandomPrincipal;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@BarracksResourceTest(controllers = MessageResource.class, outputDir = "build/generated-snippets/messages")
public class MessageResourceConfigurationTest {

    private static final Endpoint SEND_MESSAGE_ENDPOINT = Endpoint.from(HttpMethod.POST, "/messages", "unitId={unitId1}&filter={filter1}&retained={retained}");

    @MockBean
    private MessageResource messageResource;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    private RandomPrincipal principal;

    @Before
    public void setUp() throws Exception {
        this.principal = new RandomPrincipal();
    }

    @Test
    public void documentSendMessage() throws Exception {
        //  Given
        final Endpoint endpoint = SEND_MESSAGE_ENDPOINT;
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        final String message = "Message we want to send to the device";
        final String unitId1 = "deviceID1";
        final String unitId2 = "deviceID2";
        final String[] unitIdArray = {unitId1, unitId2};
        final String filter1 = "filter1";
        final String filter2 = "filter2";
        final String[] filterArray = {filter1, filter2};


        doNothing().when(messageResource).sendMessage(message, unitIdArray, filterArray, true, principal);

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.request(endpoint.getMethod(), endpoint.getPath())
                        .principal(principal)
                        .accept(MediaType.TEXT_PLAIN_VALUE)
                        .contentType(MediaType.TEXT_PLAIN_VALUE)
                        .param("unitId", unitId1 + "," + unitId2)
                        .param("filter", filter1 + "," + filter2)
                        .param("retained", "true")
                        .content(message)
        );

        // Then
        verify(messageResource).sendMessage(message, unitIdArray, filterArray, true, principal);
        result.andExpect(status().isOk())
                .andDo(document(
                        "send",
                        requestParameters(
                                parameterWithName("unitId").description("The ID of the device we want to send a message to."),
                                parameterWithName("filter").description("The filters which group the devices we want to send a message to."),
                                parameterWithName("retained").description("Indicates whether we want to retain the message or not.")
                        )
                ));
    }

    @Test
    public void documentSendMessageToAll() throws Exception {
        //  Given
        final Endpoint endpoint = SEND_MESSAGE_ENDPOINT;
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        final String message = "Message we want to send to all devices";
        final String[] emptyArray = new String[0];

        doNothing().when(messageResource).sendMessage(message, emptyArray, emptyArray, true, principal);

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.request(endpoint.getMethod(), endpoint.getPath())
                        .principal(principal)
                        .accept(MediaType.TEXT_PLAIN_VALUE)
                        .contentType(MediaType.TEXT_PLAIN_VALUE)
                        .param("retained", "true")
                        .content(message)
        );

        // Then
        verify(messageResource).sendMessage(message, emptyArray, emptyArray, true, principal);
        result.andExpect(status().isOk())
                .andDo(document(
                        "send-to-all"
                ));
    }

}
