package org.nuxeo.sample.etl.load.test;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.sample.etl.load.LoadOperation;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.runtime.stream", "org.nuxeo.ecm.core.io", "org.nuxeo.importer.stream", "org.nuxeo.sample.etl.load",
        "org.nuxeo.ecm.automation.core" })
@LocalDeploy({"org.nuxeo.sample.etl:test-stream-contrib.xml", "org.nuxeo.sample.etl:test-kafka-config-contrib.xml"})
public class TestLoad {

    @Inject
    CoreSession session;

    @Inject
    AutomationService automationService;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    public void load(boolean useKafka) throws Exception {
        final int NB_THREADS = 4;
        OperationContext ctx = new OperationContext(session);

        Map<String, Object> params = new HashMap<>();
        if (useKafka) {
            params.put("logConfig", "kafka");
        } else {
            params.put("logConfig", "chronicle");
        }
        params.put("logName", "extract");
        params.put("rootFolder", "/");
        params.put("waitMessageTimeoutSeconds", "1");
        params.put("nbThreads", NB_THREADS);

        automationService.run(ctx, LoadOperation.ID, params);
    }

    @Test
    public void testLoadFromKafka () throws Exception {
        System.out.println("Using Kafka");
        load(true);
    }

    @Test
    public void testLoadFromChronicle () throws Exception {
        System.out.println("Using Chronicle Queue");
        load(false);
    }

}
