package com.github.dfornika;

import com.github.jmchilton.blend4j.galaxy.beans.*;
import com.github.jmchilton.blend4j.galaxy.*;
import com.google.common.io.Resources;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class RunWorkflowTest {
    private static final String url = "http://localhost";
    private static final String apiKey = "admin";

    private GalaxyInstance instance = GalaxyInstanceFactory.get(url, apiKey);
    private WorkflowsClient workflowsClient = instance.getWorkflowsClient();
    private HistoriesClient historyClient = instance.getHistoriesClient();

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

        HistoryDataset input1HistoryDataset = new HistoryDataset();
        input1HistoryDataset.setSource(HistoryDataset.Source.LIBRARY);
        input1HistoryDataset.setContent(input1);
        HistoryDataset input2HistoryDataset = new HistoryDataset();
        input2HistoryDataset.setSource(HistoryDataset.Source.LIBRARY);
        input1HistoryDataset.setContent(input2);

        historyClient.createHistoryDataset(matchingHistory.getId(), input1HistoryDataset);
        historyClient.createHistoryDataset(matchingHistory.getId(), input2HistoryDataset);

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
