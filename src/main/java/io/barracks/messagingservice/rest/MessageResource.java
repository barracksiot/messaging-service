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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/messages")
public class MessageResource {

    private final MessagingServiceManager messagingServiceManager;

    @Autowired
    public MessageResource(MessagingServiceManager messagingServiceManager) {
        this.messagingServiceManager = messagingServiceManager;
    }

    @RequestMapping(method = RequestMethod.POST)
    public void sendMessage(@RequestBody String message,
                            @RequestParam(value="unitId", defaultValue = "") String[] unitIdArray,
                            @RequestParam(value="filter", defaultValue = "") String[] filtersArray,
                            @RequestParam(value = "retained", defaultValue = "false") boolean retained,
                            Principal principal) {
        final User user = ((UserAuthentication) principal).getDetails();
        final List<String> unitIdList = Arrays.asList(unitIdArray);
        final List<String> filtersList = Arrays.asList(filtersArray);
        messagingServiceManager.sendMessage(user, unitIdList, filtersList, message, retained);
    }

}
