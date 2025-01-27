# Solace REST AsyncAPI Importer for Event Portal
This project provides the ability to import AsyncApi specs into Solace Event Portal utilizing a RESTful web service. 

## What is imported
- AsyncApi Schemas &rarr; Event Portal Schema Objects
- Parameters with value lists &rarr; Event Portal Enums
- Channel + Message Definitions - with Schemas and Enums &rarr; Event Portal Events
- AsyncApi spec title &rarr; Event Portal Applications

### Details and Limitations
- AsyncApi specs must be in 2.X format. AsyncApi 3.X support is planned by unimplemented.
- Only Channels with **subscribe** operations (evaluated to events) will be imported
- Only Schemas, and Parameters (evaluated to enums) referenced by channels with **subscribe** operations will be imported.
- Given that (AsyncApi 2.X) messages, schema names, and parameter names are key fields in the specifications, changes to these fields between specs and import operations will result in new objects.

# Building the Service

## Prerequisites
You will need the following tools installed locally
- **Java JDK 17+**
- **Maven 3.8+**
- **Git**

Ths project has the following external dependency. This artifact must be installed to your Maven repo to build the projec successfully.
- **com.solace.ep.asyncapi:asyncapi-importer-core**

## Steps to build the project
Alternatively, you can find the latest release in the GitHub repository and download the jar file.

This project has a dependency on **asyncapi-importer-core** artifact, which can be built from a module contained in the [Solace AsyncAPI Importer for Event Portal](https://github.com/SolaceLabs/sol-ep-asyncapi-importer "Open project on GitHub") project.

1. If you do not have the **asyncapi-importer-core** module installed in your Maven repo, then clone the project at: [Solace AsyncAPI Importer for Event Portal](https://github.com/SolaceLabs/sol-ep-asyncapi-importer "Open project on GitHub") and follow the instructions to build it. (Run at the project root: `mvn clean install -DskipTests=true`)
    - This operation will build the CORE import capability and a CLI wrapper. The completed artifacts will be installed to your Maven repo. The CORE module (asyncapi-importer-core) is used by the REST service.
2. Build the REST Service project. At the this project root, execute: `mvn clean package`
    - This will produce compiled executable Jar at `target/asyncapi-importer-rest-[Current SemVer].jar

# Executing the Service

## Executing the Java jar file directly
You can execute the service by simply running the jar.

For example (from project root folder):<br>
`java -jar target/asyncapi-importer-rest.jar`

Using Spring Boot plugin<br>
`mvn spring-boot:run`

When running, the configured listener port is `9004`.

## Docker / Podman
A **Dockerfile** is provided to build an image from `openjdk:17-jdk-slim` base image. To build and run the image:
- docker build -t asyncapi-importer-rest:latest .
- docker run -p 9004:9004 asyncapi-importer-rest:latest

Substitute port and tags if necessary.

## SAP BTP CloudFoundry
For convenience's sake, it is also possible to directly deploy the release artifact into SAP BTP CloudFoundry space if your organization has one enabled by following the below instructions.

1. Make sure that you have the CloudFoundry CLI installed.
2. Download the release artifact over here: Github-Releases
3. Download the manifest file included as a part of the source code over here: Manifest file
4. Make sure that the above two files are stored in the same directory/folder
5. Update the route in the below section as suitable for your organization space and settings:
    ```yaml
    routes:
        - route: TARGET-ROUTE
    ```
6. Login to your organization's SAP BTP Cloudfoundry space.
7. Navigate to the location where the artifact release and manifest file are stored in a terminal/shell and run the following command:
    ```bash
    cf push -f manifest.yml
    ```
8. Monitor the deployment and startup of the application
9. Navigate with the application and click on the route defined in the application to access the tool.

# REST Service Operations
There are four operations supported by this service:
1. **Alive Check** - Check if the service is active and accessible
2. **Event Portal Token Validation** - Validate your EP bearer token for your regional endpoint
3. **Query Application Domains** - Retrieve a list of application domains in your account
4. **AsyncApi Import Request** - Import an AsyncApi spec into Event Portal

## 1. Alive Check
A simple HTTP GET request can be performed to verify that the service is active. Context is `/importer/alive`. e.g. http://localhost:9004/importer/alive on local machine.

## 2. Event Portal Token Validation
The purpose of this operation is to verify that the token being used is valid. Token validation is specific to the [Solace Cloud API endpoint](https://api.solace.dev/cloud/reference/using-the-v2-rest-apis-for-pubsub-cloud) region called. See the referenced link for more details. This is a convenience method; it is not required to verify a token before attempting to call the import method. The context for this operation is `/importer/validate-token` and the HTTP verb is `POST`.

### EP Token Request Body ###
The request body is a simple JSON message with a single field: `epToken`. `epToken` **must be Base64 encoded** in the message body. 

#### Sample JSON Request body for EP Token verification
```json
{
    "epToken": "ZXlKaGJHY2lPaUp [redacted] UQ=="
}
```

### EP Token Validation URL parameters
There are two parameters on the URL: **Solace Cloud API Region and URL Override**. These parameters are used to specify the Solace Cloud API endpoint. They are NOT required; if not specified, then the API endpoint defaults to the **US region**. If both are specified, then `urlOverride` will be used. The US, EU, AU, and SG values for `urlRegion` correlate to regional cloud API endpoints found at: [Using PubSub+ Cloud REST APIs](https://api.solace.dev/cloud/reference/using-the-v2-rest-apis-for-pubsub-cloud) 
    - `urlRegion`=[ US, EU, AU, SG ]
    - `urlOverride`=https://your.override.url

## 3. Query Application Domains
Returns a list of application domains, including application domain ID and the name. Either the name or the domain ID may be used to execute an import operation. (An Event Portal Application Domain must be specified as the target for an AsyncApi import operation)

### Application Domain Query Request Parameters
The parameters used for this operation are identical to those used for the EP Token Validation (see above).

### Application Domain Query - Sample Response Body
```json
{
    "msgs": [],
    "applicationDomains": [
        {
            "id": "xyzahd90abc",
            "name": "ACME Bank"
        },
        {
            "id": "abc1234abc3",
            "name": "Fulfillment"
        }
    ]
}
```

## 4. AsyncApi Import Request
Requests to import AsyncApi specs are executing using HTTP/POST. The context is `/importer`. e.g. `http://localhost:9004/importer` on localhost. Three things are always required to execute an import operation:
- **Application Domain** to target for import
- **AsyncAPI spec** to import
- **Event Portal Token** with sufficient write priviliges to read/write Applications, Events, Schemas, and Enums in the target application domain
Application Domain is specified as a URL parameter, Event Portal Token and the AsyncApi spec are passed in the body of the POST request.

### AsyncApi Import Request Body
The request body schema is located here: [import-request.json](src/main/resources/schemas/import-request.json)
It is a simple JSON document with 2 fields: `epToken` and `asyncApiSpec`. **Both fields are required and must be Base64 encoded.** The encoded AsyncApi spec can be in either JSON or YAML formats.

#### Sample Request Body
```json
{
    "epToken": "ZXlKaGJHY2lPaUp [redacted] UQ==",
    "asyncApiSpec": "ewoJImFzeW [redacted] Cg=="
}
```

### Import Request URL Parameters

1. **Application Domain (REQUIRED)** - Specifies the Application Domain targeted for the import operation. One of the following URL parameters must be specified. If both are values are specified, then **appDomainId** will be used:
    - `appDomainId`=Domain ID
    - `appDomainName`=Domain Name
2. **Region/URL** - These parameters are used to specify the Solace Cloud API endpoint. They are NOT required; if not specified, then the API endpoint defaults to the **US region**. If both are specified, then urlOverride will be used. The US, EU, AU, and SG values for urlRegion correlate to regional cloud API endpoints found at: [Using PubSub+ Cloud REST APIs](https://api.solace.dev/cloud/reference/using-the-v2-rest-apis-for-pubsub-cloud) 
    - `urlRegion`=[ US, EU, AU, SG ]
    - `urlOverride`=https://your.override.url
3. **New Version Strategy** - Defines how semantic versions of new objects are to be incremented. Not required, defaults to MAJOR. The first version for a new object will always be `1.0.0`
    - `newVersionStrategy`= [ MAJOR | MINOR | PATCH ]
4. **Import Options** - Neither value is required, both default to **false**. If `eventsOnly`=true is specified, then the import operation will not attempt to match, create, or update an application. Only Enums, Schemas, and Events will be imported. If `disableCascadeUpdate`=true is specified, then the creation of new Event Versions and new Application Versions based upon changes to dependencies will be disabled.
    - `eventsOnly`=true
    - `disableCascadeUpdate`=true

## Response Message

### HTTP Response Codes
- 200 - OK (Import Successful)
- 400 - BAD REQUEST (Something wrong with the input)
- 500 - INTERNAL SERVER ERROR (Something went wrong as reported by Solace Cloud API / Event Portal)

### Response Body
The response body will contain a list of text messages that account for the operations performed by the importer and any errors that occurred.

### Sample Responses

#### Successful Import
```json
{
    "msgs": [
        "INFO  - ASYNCAPI SPEC IMPORT -- START",
        "INFO  - SemVer of new object versions will increment PATCH version of the previous object",
        "INFO  - Target Solace Cloud API URL: https://api.solace.cloud",
        "INFO  - Discovered objects in AsyncApi for import to EP App Domain: test-importer",
        "INFO  - Counts -- Applications: 1 -- Enums: 0 -- Schemas: 1 -- Events: 1",
        "INFO  - Schema Object Name [/SAP_AEM/MM_MATERIAL] not found in Event Portal",
        "INFO  - CREATED Schema Object [/SAP_AEM/MM_MATERIAL] in Event Portal",
        "INFO  - CREATED Schema Version: [/SAP_AEM/MM_MATERIAL] v1.0.0 in Event Portal",
        "INFO  - Event Object Name [MATERIAL_CHANGE] not found in Event Portal",
        "INFO  - CREATED Event Object [MATERIAL_CHANGE] in Event Portal",
        "INFO  - CREATED Event Version: [MATERIAL_CHANGE] v1.0.0 in Event Portal",
        "INFO  - Application [MATERIAL_CHANGE] not found in Event Portal",
        "INFO  - CREATED Application [MATERIAL_CHANGE] in Event Portal",
        "INFO  - CREATED New Application Version: [MATERIAL_CHANGE] v1.0.0 in Event Portal",
        "INFO  - ASYNCAPI SPEC IMPORT -- COMPLETE"
    ]
}
```

#### Input Validation Errors
```json
{
    "msgs": [
        "INFO  - ASYNCAPI SPEC IMPORT -- START",
        "ERROR - One of 'appDomainId' or 'appDomainName' must be specified on the request",
        "ERROR - 'newVersionStrategy' must be one of: ['MAJOR', 'MINOR', 'PATCH'] if specified; 'MAJOR' is the default",
        "ERROR - solaceCloudApi region must be one of: ['US', 'EU', 'AU', 'SG'] if specified; 'US' is the default",
        "ERROR - ASYNCAPI SPEC IMPORT -- FAILED VALIDATION"
    ]
}
```

### Response Schema

Response schema can be found here [import-response.json](src/main/resources/schemas/import-response.json)

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "msgs": {
      "type": "array",
      "items": {
        "type": "string"
      }
    }
  },
  "required": ["msgs"],
  "additionalProperties": false
}
```

## Resources
This is not an officially supported Solace product.

For more information, try these resources:

- Ask the [Solace Community](https://solace.community)
- The Solace Developer Portal website at: https://solace.dev

## Authors

See the list of [contributors](https://github.com/SolaceLabs/asyncapi-importer-rest/graphs/contributors) who participated in this project.

# License
See the [LICENSE](LICENSE) file for details.
