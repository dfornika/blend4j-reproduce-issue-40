package com.github.dfornika;

import com.github.jmchilton.blend4j.galaxy.beans.*;
import com.github.jmchilton.blend4j.galaxy.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URISyntaxException;
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
