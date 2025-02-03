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

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * Class defing repsonse to HTTP/POST '/importer/appdomains' operation
 * contains an array of application domain ID/Name objects
 */
@Data
public class AsyncApiImportAppDomainResponse {

    private List<String> msgs;

    private List<AppDomainItem> applicationDomains;     // List of application domain objects

    public List<AppDomainItem> getApplicationDomains() {
        if (this.applicationDomains == null) {
            this.applicationDomains = new ArrayList<>();
        }
        return this.applicationDomains;
    }

    public List<String> getMsgs() {
        if (this.msgs == null) {
            this.msgs = new ArrayList<>();
        }
        return this.msgs;
    }

    @Data
    public static class AppDomainItem {

        private String id;      // Event Portal Application Domain ID

        private String name;    // Event Portal Application Domain Name

        public AppDomainItem(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
