package com.github.dfornika;

import com.github.jmchilton.blend4j.galaxy.beans.*;
import com.github.jmchilton.blend4j.galaxy.*;
import com.google.common.io.Resources;
import com.sun.jersey.api.client.ClientResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class RunWorkflowTest {
    private static final String url = "http://localhost";
    private static final String apiKey = "admin";

    private GalaxyInstance instance = GalaxyInstanceFactory.get(url, apiKey);
    private WorkflowsClient workflowsClient = instance.getWorkflowsClient();
    private HistoriesClient historyClient = instance.getHistoriesClient();
    private LibrariesClient librariesClient = instance.getLibrariesClient();

    @Before
    public void setup() {
        historyClient.create(new History("TestHistory1"));

        URL workflowUrl = Resources.getResource("TestWorkflow1.ga");
        URL input1Url = Resources.getResource("Input1.tsv");
        URL input2Url = Resources.getResource("Input2.tsv");
        String workflow = null;
        String input1 = null;
        String input2 = null;

        try {
            workflow = Resources.toString(workflowUrl, StandardCharsets.UTF_8);
            input1 = Resources.toString(input1Url, StandardCharsets.UTF_8);
            input2 = Resources.toString(input2Url, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        History matchingHistory = null;
        for(final History history : historyClient.getHistories()) {
            if(history.getName().equals("TestHistory1")) {
                matchingHistory = history;
            }
        }

        Library tmpLibrary = new Library("TestLibrary1");
        Library testLibrary = librariesClient.createLibrary(tmpLibrary);

        FileLibraryUpload input1FileLibraryUpload = new FileLibraryUpload();

        URI input1Uri = null;
        try {
            input1Uri = input1Url.toURI();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        File input1File = new File(input1Uri);
        input1FileLibraryUpload.setFile(input1File);
        input1FileLibraryUpload.setContent(input1);
        input1FileLibraryUpload.setFileType("tabular");
        input1FileLibraryUpload.setName("Input1");
        String testLibraryFolderId = librariesClient.getRootFolder(testLibrary.getId()).getId();
        input1FileLibraryUpload.setFolderId(testLibraryFolderId);

        ClientResponse upload1Response = librariesClient.uploadFile(testLibrary.getId(), input1FileLibraryUpload);

        FileLibraryUpload input2FileLibraryUpload = new FileLibraryUpload();

        URI input2Uri = null;
        try {
            input2Uri = input2Url.toURI();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        File input2File = new File(input2Uri);
        input2FileLibraryUpload.setFile(input2File);
        input2FileLibraryUpload.setContent(input2);
        input2FileLibraryUpload.setFileType("tabular");
        input2FileLibraryUpload.setName("Input2");
        input2FileLibraryUpload.setFolderId(testLibraryFolderId);
        ClientResponse upload2Response = librariesClient.uploadFile(testLibrary.getId(), input2FileLibraryUpload);

        List<LibraryContent> testLibraryContents = librariesClient.getLibraryContents(testLibrary.getId());
        for(LibraryContent testLibraryContent:testLibraryContents){
            if(testLibraryContent.getType().equals("file")) {
                HistoryDataset inputHistoryDataset = new HistoryDataset();
                inputHistoryDataset.setSource(HistoryDataset.Source.LIBRARY);
                inputHistoryDataset.setContent(testLibraryContent.getId());
                HistoryDetails inputHistoryDetails = historyClient.createHistoryDataset(matchingHistory.getId(), inputHistoryDataset);
            }
        }

        workflowsClient.importWorkflow(workflow);
    }

    @Test
    public void testRunWorkflow() {

        History matchingHistory = null;
        for(final History history : historyClient.getHistories()) {
            if(history.getName().equals("TestHistory1")) {
                matchingHistory = history;
            }
        }
        Assert.assertNotNull(matchingHistory);
        String input1Id = null;
        String input2Id = null;
        for(final HistoryContents historyDataset :historyClient.showHistoryContents(matchingHistory.getId())) {
            if(historyDataset.getName().equals("Input1")) {
                input1Id = historyDataset.getId();
            }
            if(historyDataset.getName().equals("Input2")) {
                input2Id = historyDataset.getId();
            }
        }

        Workflow matchingWorkflow = null;
        for(Workflow workflow : workflowsClient.getWorkflows()) {
            if(workflow.getName().equals("TestWorkflow1")) {
                matchingWorkflow = workflow;
            }
        }

        final WorkflowDetails workflowDetails = workflowsClient.showWorkflow(matchingWorkflow.getId());
        String workflowInput1Id = null;
        String workflowInput2Id = null;
        for(final Map.Entry<String, WorkflowInputDefinition> inputEntry : workflowDetails.getInputs().entrySet()) {
            final String label = inputEntry.getValue().getLabel();
            if(label.equals("WorkflowInput1")) {
                workflowInput1Id = inputEntry.getKey();
            }
            if(label.equals("WorkflowInput2")) {
                workflowInput2Id = inputEntry.getKey();
            }
        }

        final WorkflowInputs inputs = new WorkflowInputs();
        inputs.setDestination(new WorkflowInputs.ExistingHistory(matchingHistory.getId()));
        inputs.setWorkflowId(matchingWorkflow.getId());
        inputs.setInput(workflowInput1Id, new WorkflowInputs.WorkflowInput(input1Id, WorkflowInputs.InputSourceType.HDA));
        inputs.setInput(workflowInput2Id, new WorkflowInputs.WorkflowInput(input2Id, WorkflowInputs.InputSourceType.HDA));
        final WorkflowOutputs output = workflowsClient.runWorkflow(inputs);
        System.out.println("Running workflow in history " + output.getHistoryId());
        for(String outputId : output.getOutputIds()) {
            System.out.println("  Workflow writing to output id " + outputId);
        }
    }
}
