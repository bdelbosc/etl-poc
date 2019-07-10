/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.sample.etl.load;

import static org.nuxeo.runtime.transaction.TransactionHelper.commitOrRollbackTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.importer.stream.consumer.DocumentMessageConsumer;
import org.nuxeo.lib.stream.pattern.consumer.AbstractConsumer;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.sample.etl.ContentMessage;

/**
 * Consumes extracted Content and produce Nuxeo document.
 *
 * @since 9.1
 */
public class ContentMessageConsumer extends AbstractConsumer<ContentMessage> {
    private static final Log log = LogFactory.getLog(DocumentMessageConsumer.class);

    protected final String rootPath;

    protected final String repositoryName;

    protected CoreSession session;

    public ContentMessageConsumer(String consumerId, String repositoryName, String rootPath) {
        super(consumerId);
        this.rootPath = rootPath;
        this.repositoryName = repositoryName;
    }

    @Override
    public void close() throws Exception {
        super.close();
        if (session != null) {
            session.close();
            TransactionHelper.commitOrRollbackTransaction();
        }
    }

    @Override
    public void begin() {
        TransactionHelper.startTransaction();
        if (session == null) {
            this.session = CoreInstance.openCoreSessionSystem(repositoryName);
        }
    }

    @Override
    public void accept(ContentMessage message) {
        // TODO: create a doc from the message
        // DocumentModel doc = session.createDocumentModel(rootPath + message.getParentPath(), message.getName(),
        // message.getType());
        // set blob and props
        // doc = session.createDocument(doc);
        // no need to session.save() !
    }

    @Override
    public void commit() {
        log.debug("commit");
        session.save();
        // TODO: here if tx is in rollback we must throw something
        commitOrRollbackTransaction();
    }

    @Override
    public void rollback() {
        log.info("rollback");
        TransactionHelper.setTransactionRollbackOnly();
        TransactionHelper.commitOrRollbackTransaction();
    }

}
