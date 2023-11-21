package com.example.fn;

import com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import com.oracle.bmc.Region;
import com.oracle.bmc.functions.FunctionsInvokeClient;
import com.oracle.bmc.functions.FunctionsManagementClient;
import com.oracle.bmc.functions.model.FunctionSummary;
import com.oracle.bmc.functions.model.ApplicationSummary;
import com.oracle.bmc.functions.requests.ListApplicationsRequest;
import com.oracle.bmc.functions.responses.ListApplicationsResponse;
import com.oracle.bmc.functions.requests.ListFunctionsRequest;
import com.oracle.bmc.functions.responses.ListFunctionsResponse;
import com.oracle.bmc.functions.requests.InvokeFunctionRequest;
import com.oracle.bmc.functions.responses.InvokeFunctionResponse;
import com.oracle.bmc.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class HelloFunction {

    private ObjectStorage objStoreClient = null;
    final ResourcePrincipalAuthenticationDetailsProvider provider
            = ResourcePrincipalAuthenticationDetailsProvider.builder().build();

    public HelloFunction() {
        try {
            objStoreClient = new ObjectStorageClient(provider);
        } catch (Throwable ex) {
            System.err.println("Failed to instantiate ObjectStorage client - " + ex.getMessage());
        }
    }
    public static class GetObjectInfo {

        private String bucketName;
        private String name;

        public String getBucketName() {
            return bucketName;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }
    public String handleRequest(GetObjectInfo objectInfo) {

        String result = "FAILED";
        final ResourcePrincipalAuthenticationDetailsProvider provider = ResourcePrincipalAuthenticationDetailsProvider.builder().build();
        final Region region = Region.US_PHOENIX_1;
        final String compartmentId = "ocid1.compartment.oc1..aaaaaaaavowbqjzmbhqxjgm";
        final String name = "oci-java-sdk-function";
        final String payload = "Hii";

        if (objStoreClient == null) {
            System.err.println("There was a problem creating the ObjectStorage Client object. Please check logs");
            return result;
        }
        try {

            String nameSpace = System.getenv().get("NAMESPACE");

            GetObjectRequest gor = GetObjectRequest.builder()
                    .namespaceName(nameSpace)
                    .bucketName(objectInfo.getBucketName())
                    .objectName(objectInfo.getName())
                    .build();
            System.err.println("Getting content for object " + objectInfo.getName() + " from bucket " + objectInfo.getBucketName());

            GetObjectResponse response = objStoreClient.getObject(gor);
            result = new BufferedReader(new InputStreamReader(response.getInputStream()))
                    .lines().collect(Collectors.joining("\n"));

            System.err.println("Finished reading content for object " + objectInfo.getName());
            invokeFunction(provider, region, compartmentId, name, payload);

        } catch (Throwable e) {
            System.err.println("Error fetching object " + e.getMessage());
            result = "Error fetching object " + e.getMessage();
        }

        //invokeFunction(provider, region, compartmentId, name, payload);

        return result;

    }

    public static FunctionSummary getUniqueFunctionByName(
            final FunctionsManagementClient fnManagementClient,
            final String compartmentId,
            final String applicationDisplayName,
            final String functionDisplayName)
            throws Exception {
        final ApplicationSummary application =
                getUniqueApplicationByName(
                        fnManagementClient, compartmentId, applicationDisplayName);
        return getUniqueFunctionByName(
                fnManagementClient, application.getId(), functionDisplayName);
    }

    public static FunctionSummary getUniqueFunctionByName(
            final FunctionsManagementClient fnManagementClient,
            final String applicationId,
            final String functionDisplayName)
            throws Exception {

        final ListFunctionsRequest listFunctionsRequest =
                ListFunctionsRequest.builder()
                        .applicationId(applicationId)
                        .displayName(functionDisplayName)
                        .build();

        final ListFunctionsResponse listFunctionsResponse =
                fnManagementClient.listFunctions(listFunctionsRequest);

        if (listFunctionsResponse.getItems().size() != 1) {
            throw new Exception(
                    "Could not find function with name "
                            + functionDisplayName
                            + " in application "
                            + applicationId);
        }

        return listFunctionsResponse.getItems().get(0);
    }

    public static ApplicationSummary getUniqueApplicationByName(
            final FunctionsManagementClient fnManagementClient,
            final String compartmentId,
            final String applicationDisplayName)
            throws Exception {
        final ListApplicationsRequest listApplicationsRequest =
                ListApplicationsRequest.builder()
                        .displayName(applicationDisplayName)
                        .compartmentId(compartmentId)
                        .build();

        final ListApplicationsResponse resp =
                fnManagementClient.listApplications(listApplicationsRequest);

        if (resp.getItems().size() != 1) {
            throw new Exception(
                    "Could not find unique application with name "
                            + applicationDisplayName
                            + " in compartment "
                            + compartmentId);
        }

        final ApplicationSummary application = resp.getItems().get(0);
        return application;
    }

    private static String invokeFunction(
            final FunctionsInvokeClient fnInvokeClient,
            final FunctionSummary function,
            final String payload)
            throws Exception {
        String response;
        try {
            System.err.println("Invoking function endpoint - " + function.getInvokeEndpoint());

            fnInvokeClient.setEndpoint(function.getInvokeEndpoint());
            final InvokeFunctionRequest invokeFunctionRequest =
                    InvokeFunctionRequest.builder()
                            .functionId(function.getId())
                            .invokeFunctionBody(
                                    StreamUtils.createByteArrayInputStream(payload.getBytes()))
                            .build();

            final InvokeFunctionResponse invokeFunctionResponse =
                    fnInvokeClient.invokeFunction(invokeFunctionRequest);

            response = "Done executing func1...";
                    //StreamUtils.toString(
                            //invokeFunctionResponse.getInputStream(), StandardCharsets.UTF_8);

        } catch (final Exception e) {
            e.printStackTrace();
            System.err.println("Failed to invoke function: " + e);
            throw e;
        }

        return response;
    }

    public static void invokeFunction(
            final ResourcePrincipalAuthenticationDetailsProvider provider,
            final Region region,
            final String compartmentId,
            final String name,
            final String payload)
            throws Exception {

        final FunctionsManagementClient fnManagementClient =
                FunctionsManagementClient.builder().region(region).build(provider);

        final FunctionsInvokeClient fnInvokeClient =
                FunctionsInvokeClient.builder().build(provider);

        try {
            final String appName = "e2e-function-demo";
            final String fnName = "java_func2";
            final FunctionSummary fn =
                    getUniqueFunctionByName(fnManagementClient, compartmentId, appName, fnName);

            final String response = invokeFunction(fnInvokeClient, fn, payload);
            if (response != null) {
                System.out.println("Response from function:  " + response);
            }
        } finally {
            fnInvokeClient.close();
            fnManagementClient.close();
        }
    }
}
