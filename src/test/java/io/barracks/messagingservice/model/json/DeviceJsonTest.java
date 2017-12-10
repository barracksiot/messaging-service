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

package io.barracks.messagingservice.model.json;

import io.barracks.messagingservice.model.Device;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
public class DeviceJsonTest {

    private static final String DEVICE_UNIT_ID = "837013ae-ef90-48ce-8813-82a361204a66";

    @Autowired
    private JacksonTester<Device> json;

    @Value("classpath:io/barracks/messagingservice/device.json")
    private Resource device;

    @Test
    public void deserialize_shouldReturnCompleteObject() throws Exception {
        // Given
        final Device expected = buildDevice();

        // When
        final Device result = json.readObject(device.getInputStream());

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void serialize_shouldIncludeAllAttributes() throws Exception {
        // Given
        final Device source = buildDevice();

        // When
        final JsonContent<Device> result = json.write(source);

        // Then
        assertThat(result).extractingJsonPathValue("unitId").isEqualTo(source.getUnitId());
    }

    private Device buildDevice() {
        return Device.builder()
                .unitId(DEVICE_UNIT_ID)
                .build();
    }
}
