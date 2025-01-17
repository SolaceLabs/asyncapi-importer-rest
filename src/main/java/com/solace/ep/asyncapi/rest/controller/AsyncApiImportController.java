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

package com.solace.ep.asyncapi.rest.controller;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.solace.ep.asyncapi.importer.AsyncApiImporter;
import com.solace.ep.asyncapi.rest.log.MemoryAppender;
import com.solace.ep.asyncapi.rest.models.AliveMessage;
import com.solace.ep.asyncapi.rest.models.AsyncApiImportRequest;
import com.solace.ep.asyncapi.rest.models.AsyncApiImportResponse;
import com.solace.ep.asyncapi.rest.utils.LogUtils;
import com.solace.ep.asyncapi.rest.utils.ValidationUtils;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class AsyncApiImportController {

    // In-memory logging
    private static LoggerContext context = LogUtils.getContext();
    private static PatternLayoutEncoder encoder = LogUtils.getMemoryEncoder(context, "%-5level - %msg");

    /**
     * For testing Server deployment checks
     * @return
     */
    @GetMapping("/importer/alive")
    public ResponseEntity<AliveMessage> alive()
    {
        log.debug("/importer/alive endpoint invoked");
        return new ResponseEntity<>(new AliveMessage(), HttpStatus.OK);
    }

    /**
     * Import operation
     * @param request
     * @param appDomainId
     * @param appDomainName
     * @param urlRegion
     * @param urlOverride
     * @param newVersionStrategy
     * @param eventsOnly
     * @param disableCascadeUpdate
     * @return
     */
    @PostMapping("/importer")
    public ResponseEntity<AsyncApiImportResponse> importAsyncApi(
        @RequestBody AsyncApiImportRequest request,
        @RequestParam(name = "appDomainId", required = false) String appDomainId,
        @RequestParam(name = "appDomainName", required = false) String appDomainName,
        @RequestParam(name = "urlRegion", defaultValue = "US") String urlRegion,
        @RequestParam(name = "urlOverride", required = false) String urlOverride,
        @RequestParam(name = "newVersionStrategy", defaultValue = "MAJOR") String newVersionStrategy,
        @RequestParam(name = "eventsOnly", defaultValue = "false") boolean eventsOnly,
        @RequestParam(name = "disableCascadeUpdate", defaultValue = "false") boolean disableCascadeUpdate
    )
    {
        // Set up memory logging for this request
        MemoryAppender memoryAppender = LogUtils.getMemoryAppender(context, encoder, Thread.currentThread().getName());
        Logger rootLogger = context.getLogger("ROOT");
        rootLogger.addAppender(memoryAppender);

        log.info("ASYNCAPI SPEC IMPORT -- START");

        // Validate the input request - parameters and body
        boolean validRequest = true;
        try {
            validRequest = ValidationUtils.validDomainIdentifiers(appDomainId, appDomainName) && validRequest;
            validRequest = ValidationUtils.validNewVersionStrategy(newVersionStrategy) && validRequest;
            validRequest = ValidationUtils.validRegion(urlRegion, urlOverride) && validRequest;
            validRequest = ValidationUtils.validRequestBody(request) && validRequest;
        } catch (Exception exc) {
            validRequest = false;
            log.error("Error caught validating request: {}", exc.getMessage());
        }

        if (!validRequest) {
            log.error("ASYNCAPI SPEC IMPORT -- FAILED VALIDATION");
            AsyncApiImportResponse response = new AsyncApiImportResponse();
            response.getMsgs().addAll(memoryAppender.getMemoryLogList());
            rootLogger.detachAppender(memoryAppender);
            memoryAppender.stop();
            memoryAppender.clear();
            memoryAppender = null;
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        log.debug("AsyncApi import request passed validation");
        if (disableCascadeUpdate) {
            log.info("Cascade Update feature is disabled for this operation");
        }
        if (eventsOnly) {
            log.info("Application Import is disabled; Operation will import Enums, Schemas, and Events");
        }
        log.info("SemVer of new object versions will increment {} version of the previous object", newVersionStrategy);
        log.debug("Thread ID: {} -- Name: {} -- Group: {}", Thread.currentThread().getId(), Thread.currentThread().getName(), Thread.currentThread().getThreadGroup().getName() );

        AsyncApiImportResponse response = new AsyncApiImportResponse();
        HttpStatus httpStatus = null;

        try {
            final boolean useAppDomainId = ( appDomainId != null && !appDomainId.isBlank() );
            final String epToken = new String(Base64.getDecoder().decode(request.getEpToken()), StandardCharsets.UTF_8);
            final String asyncApiSpec = new String(Base64.getDecoder().decode(request.getAsyncApiSpec()), StandardCharsets.UTF_8);
            final String resolvedUrl = ValidationUtils.getUrlByRegion(urlRegion, urlOverride);

            log.info("Target Solace Cloud API URL: {}", resolvedUrl);

            if (useAppDomainId)
            {
                // TODO - Implement select on application Id
                // log.info("Resolving Application domain by appDomainId is Not Implemented");

                AsyncApiImporter.execImportOperation(
                    appDomainId, 
                    null, 
                    epToken, 
                    asyncApiSpec, 
                    resolvedUrl, 
                    newVersionStrategy
                );
                log.info("ASYNCAPI SPEC IMPORT -- COMPLETE");
            } else {
                AsyncApiImporter.execImportOperation(
                    null,
                    appDomainName, 
                    epToken, 
                    asyncApiSpec, 
                    resolvedUrl,
                    newVersionStrategy, 
                    disableCascadeUpdate,
                    eventsOnly
                );
                log.info("ASYNCAPI SPEC IMPORT -- COMPLETE");
            }
            httpStatus = HttpStatus.OK;
        } catch (Exception exc) {
            log.error("ASYNCAPI SPEC IMPORT FAILED WITH AN ERROR");
            log.error(exc.getLocalizedMessage());
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        } finally {
            response.getMsgs().addAll(memoryAppender.getMemoryLogList());
            rootLogger.detachAppender(memoryAppender);
            memoryAppender.stop();
            memoryAppender.clear();
            memoryAppender = null;
        }
        return new ResponseEntity<>(response, (httpStatus == null ? HttpStatus.INTERNAL_SERVER_ERROR : httpStatus));
    }
}
