/*
 * Copyright 2004-2006 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.compass.spring.transaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.compass.core.CompassException;
import org.compass.core.impl.InternalCompassSession;
import org.compass.core.transaction.AbstractTransaction;
import org.compass.core.transaction.CompassSessionHolder;
import org.compass.core.transaction.TransactionException;
import org.compass.core.transaction.TransactionSessionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class SpringSyncTransaction extends AbstractTransaction {

	private static final Log log = LogFactory.getLog(SpringSyncTransaction.class);

	private TransactionStatus status;

	private boolean controllingNewTransaction = false;

	private boolean commitFailed;

	private PlatformTransactionManager transactionManager;

	public void begin(PlatformTransactionManager transactionManager, InternalCompassSession session,
			TransactionIsolation transactionIsolation, boolean commitBeforeCompletion) {
		if (log.isDebugEnabled()) {
			log.debug("Beginning Spring sycn transaction, and a new compass transaction");
		}

		this.transactionManager = transactionManager;

		// the factory called begin, so we are in charge, if we were not, than
		// it would not call begin
		controllingNewTransaction = true;

		if (transactionManager != null) {
			DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
			transactionDefinition.setPropagationBehavior(DefaultTransactionDefinition.PROPAGATION_REQUIRED);
			boolean readOnly = false;
			if (transactionIsolation == TransactionIsolation.READ_ONLY_READ_COMMITTED) {
				readOnly = true;
			}
			transactionDefinition.setReadOnly(readOnly);
			status = transactionManager.getTransaction(transactionDefinition);
		}

		session.getSearchEngine().begin(transactionIsolation);

		SpringTransactionSynchronization sync;
		if (transactionManager != null) {
			sync = new SpringTransactionSynchronization(session, status.isNewTransaction(), commitBeforeCompletion);
		} else {
			sync = new SpringTransactionSynchronization(session, false, commitBeforeCompletion);
		}
		TransactionSynchronizationManager.registerSynchronization(sync);

		setBegun(true);
	}

	protected void doCommit() throws CompassException {
		if (!controllingNewTransaction) {
			if (log.isDebugEnabled()) {
				log.debug("Not controlling the new transaction creation");
			}
			return;
		}

		if (transactionManager == null) {
			// do nothing, it could only get here if the spring transaction was
			// started and we synch on it
			return;
		}

		if (status.isNewTransaction()) {
			if (log.isDebugEnabled()) {
				log.debug("Committing transaction started by us");
			}
			try {
				transactionManager.commit(status);
			} catch (Exception e) {
				commitFailed = true;
				// so the transaction is already rolled back, by JTA spec
				log.error("Commit failed", e);
				throw new TransactionException("Commit failed", e);
			}
		} else {
			if (log.isDebugEnabled()) {
				log.debug("Commit called, let Spring synchronization work for us");
			}
		}
	}

	protected void doRollback() throws CompassException {

		if (transactionManager == null) {
			// do nothing, it could only get here if the spring transaction was
			// started and we synch on it
			return;
		}

		try {
			if (status.isNewTransaction()) {
				if (log.isDebugEnabled()) {
					log.debug("Rolling back transaction started by us");
				}
				if (!commitFailed)
					transactionManager.rollback(status);
			} else {
				if (log.isDebugEnabled()) {
					log.debug("Roll back called, mark it as rolled back");
				}
				status.setRollbackOnly();
			}
		} catch (Exception e) {
			log.error("Rollback failed", e);
			throw new TransactionException("Rollback failed with exception", e);
		}
	}

	public boolean wasRolledBack() throws CompassException {
		throw new TransactionException("Not supported");
	}

	public boolean wasCommitted() throws CompassException {
		throw new TransactionException("Not supported");
	}

	public static class SpringTransactionSynchronization implements TransactionSynchronization {

		private InternalCompassSession session;

		private boolean compassControledTransaction;

		private boolean commitBeforeCompletion;

		public SpringTransactionSynchronization(InternalCompassSession session, boolean compassControledTransaction,
				boolean commitBeforeCompletion) {
			this.session = session;
			this.compassControledTransaction = compassControledTransaction;
			this.commitBeforeCompletion = commitBeforeCompletion;
		}

		public void suspend() {
		}

		public void resume() {
		}

		public void beforeCommit(boolean readOnly) {
		}

		public void beforeCompletion() {
			if (!commitBeforeCompletion) {
				return;
			}
			session.getSearchEngine().commit(true);
		}

		public void afterCompletion(int status) {
			try {
				if (status == STATUS_COMMITTED) {
					if (log.isDebugEnabled()) {
						log.debug("Committing compass transaction using spring synchronization");
					}
					if (!commitBeforeCompletion) {
						session.getSearchEngine().commit(true);
					}
				} else if (status == STATUS_ROLLED_BACK) {
					if (log.isDebugEnabled()) {
						log.debug("Rolling back compass transaction using spring synchronization");
					}
					session.getSearchEngine().rollback();
				}
			} catch (Exception e) {
				log.error("Exception occured when sync with transaction", e);
				// TODO swallow??????
			} finally {
				CompassSessionHolder holder = TransactionSessionManager.getHolder(session.getCompass());
				holder.removeSession(this);
				session.evictAll();
				// close the session AFTER we cleared it from the transaction,
				// so it will be actually closed. Also close it only if we do
				// not contoll the transaction
				// (otherwise it will be closed by the calling template)
				if (!compassControledTransaction) {
					session.close();
				}
			}
		}

	}

}
