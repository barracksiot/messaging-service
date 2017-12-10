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

package io.barracks.messagingservice.rest;

import io.barracks.messagingservice.manager.MessagingServiceManager;
import io.barracks.messagingservice.model.User;
import io.barracks.messagingservice.security.UserAuthentication;
import io.barracks.messagingservice.utils.RandomPrincipal;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.scheduling.annotation.AsyncResult;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class MessageResourceTest {

    @Mock
    private MessagingServiceManager messagingServiceManager;

    private MessageResource messageResource;

    private Principal principal = new RandomPrincipal();

    @Before
    public void setUp() {
        messageResource = new MessageResource(messagingServiceManager);
    }

    @Test
    public void sendMessage_whenUnitIdAndFiltersProvided_shouldCallManager() {
        //Given
        final String unitId1 = UUID.randomUUID().toString();
        final String unitId2 = UUID.randomUUID().toString();
        final String filter1 = UUID.randomUUID().toString();
        final String filter2 = UUID.randomUUID().toString();
        final String[] unitIdArray = {unitId1, unitId2};
        final String[] filtersArray = {filter1, filter2};
        final List<String> unitIdList = Arrays.asList(unitIdArray);
        final List<String> filtersList = Arrays.asList(filtersArray);
        final boolean retained = true;
        final String message = UUID.randomUUID().toString();
        final User user = ((UserAuthentication) principal).getDetails();

        doReturn(new AsyncResult<>(true)).when(messagingServiceManager).sendMessage(user, unitIdList, filtersList, message, retained);

        //When
        messageResource.sendMessage(message, unitIdArray, filtersArray, retained, principal);

        //Then
        verify(messagingServiceManager).sendMessage(user, unitIdList, filtersList, message, retained);
    }

    @Test
    public void sendMessage_whenNoUnitIdAndFiltersProvided_shouldCallManager() {
        //Given
        final String filter1 = UUID.randomUUID().toString();
        final String filter2 = UUID.randomUUID().toString();
        final String[] emptyUnitIdArray = new String[0];
        final String[] filtersArray = {filter1, filter2};
        final List<String> unitIdList = Arrays.asList(emptyUnitIdArray);
        final List<String> filtersList = Arrays.asList(filtersArray);
        final boolean retained = true;
        final String message = UUID.randomUUID().toString();
        final User user = ((UserAuthentication) principal).getDetails();

        doReturn(new AsyncResult<>(true)).when(messagingServiceManager).sendMessage(user, unitIdList, filtersList, message, retained);

        //When
        messageResource.sendMessage(message, emptyUnitIdArray, filtersArray, retained, principal);

        //Then
        verify(messagingServiceManager).sendMessage(user, unitIdList, filtersList, message, retained);
    }

    @Test
    public void sendMessage_whenUnitIdProvidedButNoFilters_shouldCallManager() {
        //Given
        final String unitId1 = UUID.randomUUID().toString();
        final String unitId2 = UUID.randomUUID().toString();
        final String[] unitIdArray = {unitId1, unitId2};
        final String[] emptyFiltersArray = new String[0];
        final List<String> unitIdList = Arrays.asList(unitIdArray);
        final List<String> filtersList = Arrays.asList(emptyFiltersArray);
        final boolean retained = true;
        final String message = UUID.randomUUID().toString();
        final User user = ((UserAuthentication) principal).getDetails();

        doReturn(new AsyncResult<>(true)).when(messagingServiceManager).sendMessage(user, unitIdList, filtersList, message, retained);

        //When
        messageResource.sendMessage(message, unitIdArray, emptyFiltersArray, retained, principal);

        //Then
        verify(messagingServiceManager).sendMessage(user, unitIdList, filtersList, message, retained);
    }

    @Test
    public void sendMessage_whenNoUnitIdOrFilters_shouldCallManager() {
        //Given
        final String message = UUID.randomUUID().toString();
        final User user = ((UserAuthentication) principal).getDetails();
        final String[] emptyArray = new String[0];
        final List<String> emptyList = Arrays.asList(emptyArray);
        final boolean retained = true;
        doReturn(new AsyncResult<>(true)).when(messagingServiceManager).sendMessage(user, emptyList, emptyList, message, retained);

        //When
        messageResource.sendMessage(message, emptyArray, emptyArray, retained, principal);

        //Then
        verify(messagingServiceManager).sendMessage(user, emptyList, emptyList, message, retained);
    }
}