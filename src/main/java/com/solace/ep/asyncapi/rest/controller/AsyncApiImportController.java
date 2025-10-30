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

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.solace.ep.asyncapi.importer.AsyncApiImporter;
import com.solace.ep.asyncapi.importer.EpImportOperator;
import com.solace.ep.asyncapi.rest.apis.SolaceCloudApiCalls;
import com.solace.ep.asyncapi.rest.log.MemoryAppender;
import com.solace.ep.asyncapi.rest.models.AliveMessage;
import com.solace.ep.asyncapi.rest.models.AsyncApiImportAppDomainResponse;
import com.solace.ep.asyncapi.rest.models.AsyncApiImportRequest;
import com.solace.ep.asyncapi.rest.models.AsyncApiImportResponse;
import com.solace.ep.asyncapi.rest.models.AsyncApiImportTokenRequest;
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
    private static AtomicInteger importRequestCounter = new AtomicInteger(0);

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
     * Defines web method to validate EP bearer token. Bearer token is passed in
     * the request body as Base64 encoded string.
     * @param request
     * @param urlRegion
     * @param urlOverride
     * @return
     */
    @PostMapping("/importer/validate-token")
    public ResponseEntity<AsyncApiImportResponse> validateToken(
        @RequestBody AsyncApiImportTokenRequest request,
        @RequestParam(name = "urlRegion", defaultValue = "US") String urlRegion,
        @RequestParam(name = "urlOverride", required = false) String urlOverride
    )
    {
        log.debug("/importer/validate-token invoked");

        String responseMessage = null;
        boolean validRequest = true;
        String epToken = null, resolvedCloudApiUrl = null;
        try {
            validRequest = ValidationUtils.isBase64(request.getEpToken()) && validRequest;
            validRequest = ValidationUtils.validRegion(urlRegion, urlOverride) && validRequest;
            if (!validRequest) {
                responseMessage = "Token not valid or Solace Cloud API not specified correctly";
            } else {
                epToken = ValidationUtils.decodeBase64(request.getEpToken());
                resolvedCloudApiUrl = ValidationUtils.getUrlByRegion(urlRegion, urlOverride);
            }
        } catch (Exception exc) {
            responseMessage = exc.getLocalizedMessage();
            validRequest = false;
        }

        if (!validRequest) {
            AsyncApiImportResponse response = new AsyncApiImportResponse();
            response.getMsgs().add(responseMessage == null ? "Unidentified Error" : responseMessage);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        return SolaceCloudApiCalls.validateEpToken(epToken, resolvedCloudApiUrl);
    }

    /**
     * Web method used to return list of application domains
     * @param request
     * @param urlRegion
     * @param urlOverride
     * @return
     */
    @PostMapping("/importer/appdomains")
    public ResponseEntity<AsyncApiImportAppDomainResponse> getDomains(
        @RequestBody AsyncApiImportTokenRequest request,
        @RequestParam(name = "urlRegion", defaultValue = "US") String urlRegion,
        @RequestParam(name = "urlOverride", required = false) String urlOverride
    )
    {
        log.debug("/importer/appdomains invoked");

        String resolvedUrl = "";
        String decodedEpToken = "";
        String responseMessage = null;

        boolean validRequest = true;
        try {
            validRequest = ValidationUtils.isBase64(request.getEpToken()) && validRequest;
            validRequest = ValidationUtils.validRegion(urlRegion, urlOverride) && validRequest;

            resolvedUrl = ValidationUtils.getUrlByRegion(urlRegion, urlOverride);
            decodedEpToken = ValidationUtils.decodeBase64(request.getEpToken());
        } catch (Exception exc) {
            validRequest = false;
            responseMessage = exc.getLocalizedMessage();
            log.warn("AsyncApiImportController.getDomains: {}", responseMessage);
        }

        if (!validRequest) {
            AsyncApiImportAppDomainResponse response = new AsyncApiImportAppDomainResponse();
            response.getApplicationDomains();
            response.getMsgs().add("Not a Base64 encoded token or could not resolve the correct Solace Cloud API URL");
            if (responseMessage != null) {
                response.getMsgs().add(responseMessage);
            }
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        return SolaceCloudApiCalls.getAppDomainsFromSolaceCloudApi(decodedEpToken, resolvedUrl);
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
        @RequestParam(name = "importApplication", defaultValue = "true") boolean importApplication,
        @RequestParam(name = "importEventApi", defaultValue = "false") boolean importEventApi,
        @RequestParam(name = "cascadeUpdate", defaultValue = "true") boolean cascadeUpdate
    )
    {
        final int thisRequestId = importRequestCounter.incrementAndGet();
        log.info("AsyncApi Import Request ID {}: /importer invoked", thisRequestId);

        // Set up memory logging for this request
        MemoryAppender memoryAppender = LogUtils.getMemoryAppender(context, encoder, Thread.currentThread().getName(), EpImportOperator.getOperatorIdPrefix(thisRequestId));
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
            rootLogger.detachAppender(memoryAppender);
            memoryAppender.stop();
            response.getMsgs().addAll(memoryAppender.getMemoryLogList());
            memoryAppender.clear();
            memoryAppender = null;
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        log.debug("AsyncApi import request passed validation");
        if (! cascadeUpdate) {
            log.info("Cascade Update feature is disabled for this operation");
        }
        if (! importApplication) {
            log.info("Application Import is disabled; Operation will import Enums, Schemas, and Events");
        }
        if (importEventApi) {
            log.info("Event API import is enabled; An Event API will be created for the imported AsyncAPI spec");
        }
        log.info("SemVer of new object versions will increment {} version of the previous object", newVersionStrategy);
        log.debug("Thread ID: {} -- Name: {} -- Group: {}", Thread.currentThread().getId(), Thread.currentThread().getName(), Thread.currentThread().getThreadGroup().getName() );

        AsyncApiImportResponse response = new AsyncApiImportResponse();
        HttpStatus httpStatus = null;

        try {
            final boolean useAppDomainId = ( appDomainId != null && !appDomainId.isBlank() );
            final String epToken = ValidationUtils.decodeBase64(request.getEpToken());
            final String asyncApiSpec = ValidationUtils.decodeBase64(request.getAsyncApiSpec());
            final String resolvedUrl = ValidationUtils.getUrlByRegion(urlRegion, urlOverride);

            log.info("Target Solace Cloud API URL: {}", resolvedUrl);

            if (useAppDomainId)
            {
                AsyncApiImporter.execImportOperation(
                    appDomainId, 
                    null, 
                    epToken, 
                    asyncApiSpec, 
                    resolvedUrl, 
                    newVersionStrategy,
                    cascadeUpdate,
                    importApplication,
                    importEventApi
                );
            } else {
                AsyncApiImporter.execImportOperation(
                    null,
                    appDomainName, 
                    epToken, 
                    asyncApiSpec, 
                    resolvedUrl,
                    newVersionStrategy,
                    cascadeUpdate,
                    importApplication,
                    importEventApi
                );
            }
            log.info("ASYNCAPI SPEC IMPORT -- COMPLETE");
            httpStatus = HttpStatus.OK;
        } catch (Exception exc) {
            log.error("ASYNCAPI SPEC IMPORT FAILED WITH AN ERROR");
            log.error(exc.getLocalizedMessage());
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        } finally {
            rootLogger.detachAppender(memoryAppender);
            memoryAppender.stop();
            response.getMsgs().addAll(memoryAppender.getMemoryLogList());
            memoryAppender.clear();
            memoryAppender = null;
        }
        return new ResponseEntity<>(response, (httpStatus == null ? HttpStatus.INTERNAL_SERVER_ERROR : httpStatus));
    }
}
