/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.solace.ep.asyncapi.rest.models;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import lombok.Data;

/**
 * Simpe return type for 'Alive' requests to indicate that the service is available
 */
@Data
public class AliveMessage {

    private String message;

    private String currentTime;

    public AliveMessage()
    {
        this.message = "Solace AsyncApi Importer REST Service";
        final ZonedDateTime currentDateTime = ZonedDateTime.now();
        final String iso8601time = currentDateTime.format(DateTimeFormatter.ISO_INSTANT);
        this.currentTime = iso8601time;
    }

}
