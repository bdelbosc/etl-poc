/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Benoit Delbosc
 */
package org.nuxeo.sample.etl.load;

import static org.nuxeo.importer.stream.automation.BlobConsumers.DEFAULT_LOG_CONFIG;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.importer.stream.consumer.DocumentConsumerPolicy;
import org.nuxeo.importer.stream.consumer.DocumentConsumerPool;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.pattern.consumer.BatchPolicy;
import org.nuxeo.lib.stream.pattern.consumer.ConsumerPolicy;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamService;
import org.nuxeo.sample.etl.ContentMessage;

import net.jodah.failsafe.RetryPolicy;

/**
 * @since 9.10
 */
@Operation(id = LoadOperation.ID, category = Constants.CAT_SERVICES, label = "Load document", since = "9.10", description = "Load content into the repository.")
public class LoadOperation {
    private static final Log log = LogFactory.getLog(LoadOperation.class);

    public static final String ID = "Load.run";

    public static final String DEFAULT_DOC_LOG_NAME = "extract";

    @Context
    protected OperationContext ctx;

    @Param(name = "nbThreads", required = false)
    protected Integer nbThreads;

    @Param(name = "rootFolder")
    protected String rootFolder;

    @Param(name = "repositoryName", required = false)
    protected String repositoryName;

    @Param(name = "batchSize", required = false)
    protected Integer batchSize = 10;

    @Param(name = "batchThresholdS", required = false)
    protected Integer batchThresholdS = 20;

    @Param(name = "retryMax", required = false)
    protected Integer retryMax = 3;

    @Param(name = "retryDelayS", required = false)
    protected Integer retryDelayS = 2;

    @Param(name = "logName", required = false)
    protected String logName;

    @Param(name = "logConfig", required = false)
    protected String logConfig;

    @Param(name = "blockIndexing", required = false)
    protected Boolean blockIndexing = false;

    @Param(name = "blockAsyncListeners", required = false)
    protected Boolean blockAsyncListeners = false;

    @Param(name = "blockPostCommitListeners", required = false)
    protected Boolean blockPostCommitListeners = false;

    @Param(name = "blockDefaultSyncListeners", required = false)
    protected Boolean blockSyncListeners = false;

    @Param(name = "useBulkMode", required = false)
    protected Boolean useBulkMode = false;

    @Param(name = "waitMessageTimeoutSeconds", required = false)
    protected Integer waitMessageTimeoutSeconds = 20;

    @OperationMethod
    public void run() {
        checkAccess(ctx);
        repositoryName = getRepositoryName();
        ConsumerPolicy consumerPolicy = DocumentConsumerPolicy.builder()
                                                              .blockIndexing(blockIndexing)
                                                              .blockAsyncListeners(blockAsyncListeners)
                                                              .blockPostCommitListeners(blockPostCommitListeners)
                                                              .blockDefaultSyncListener(blockSyncListeners)
                                                              .useBulkMode(useBulkMode)
                                                              .name(ID)
                                                              .batchPolicy(BatchPolicy.builder()
                                                                                      .capacity(batchSize)
                                                                                      .timeThreshold(Duration.ofSeconds(
                                                                                              batchThresholdS))
                                                                                      .build())
                                                              .retryPolicy(new RetryPolicy().withMaxRetries(retryMax)
                                                                                            .withDelay(retryDelayS,
                                                                                                    TimeUnit.SECONDS))
                                                              .maxThreads(getNbThreads())
                                                              .waitMessageTimeout(
                                                                      Duration.ofSeconds(waitMessageTimeoutSeconds))
                                                              //.salted()
                                                              .build();
        log.warn(String.format("Import documents from log: %s into: %s/%s, with policy: %s", getLogName(),
                repositoryName, rootFolder, consumerPolicy));
        StreamService service = Framework.getService(StreamService.class);
        LogManager manager = service.getLogManager(getLogConfig());
        try (DocumentConsumerPool<ContentMessage> consumers = new DocumentConsumerPool<>(getLogName(), manager,
                new ContentMessageConsumerFactory(repositoryName, rootFolder), consumerPolicy)) {
            consumers.start().get();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    protected short getNbThreads() {
        if (nbThreads != null) {
            return nbThreads.shortValue();
        }
        return 0;
    }

    protected String getRepositoryName() {
        if (repositoryName != null && !repositoryName.isEmpty()) {
            return repositoryName;
        }
        return ctx.getCoreSession().getRepositoryName();
    }

    protected String getLogName() {
        if (logName != null) {
            return logName;
        }
        return DEFAULT_DOC_LOG_NAME;
    }

    protected String getLogConfig() {
        if (logConfig != null) {
            return logConfig;
        }
        return DEFAULT_LOG_CONFIG;
    }

    protected static void checkAccess(OperationContext context) {
        NuxeoPrincipal principal = (NuxeoPrincipal) context.getPrincipal();
        if (principal == null || !principal.isAdministrator()) {
            throw new RuntimeException("Unauthorized access: " + principal);
        }
    }
}
