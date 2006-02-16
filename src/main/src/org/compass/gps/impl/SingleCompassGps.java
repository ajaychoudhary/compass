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

package org.compass.gps.impl;

import java.util.Iterator;
import java.util.Properties;

import org.compass.core.Compass;
import org.compass.core.CompassCallback;
import org.compass.core.CompassException;
import org.compass.core.CompassTemplate;
import org.compass.core.CompassTransaction;
import org.compass.core.CompassTransaction.TransactionIsolation;
import org.compass.core.config.CompassEnvironment;
import org.compass.core.config.CompassSettings;
import org.compass.core.engine.SearchEngineException;
import org.compass.core.engine.SearchEngineIndexManager;
import org.compass.core.impl.InternalCompass;
import org.compass.core.lucene.engine.LuceneSearchEngineFactory;
import org.compass.gps.CompassGpsDevice;
import org.compass.gps.CompassGpsException;

/**
 * A {@link org.compass.gps.CompassGps} implementation that holds a
 * single <code>Compass</code> instance. The <code>Compass</code> instance
 * is used for both the index operation and the mirror operation. <p/> When
 * executing the mirror operation, the implementation will not use the
 * configured transaction isolation, but will use the
 * {@link #setIndexTransactionIsolation(CompassTransaction.TransactionIsolation)}
 * transaction isolation, which defaults to <code>batch_insert</code>.
 * 
 * @author kimchy
 */
public class SingleCompassGps extends AbstractCompassGps {

	private Compass compass;

	private CompassTemplate compassTemplate;

	private Compass indexCompass;

	private CompassTemplate indexCompassTemplate;

	private CompassTransaction.TransactionIsolation indexTransactionIsolation = CompassTransaction.TransactionIsolation.BATCH_INSERT;

	private Properties indexSettings;

	private CompassSettings indexCompassSettings;

	public SingleCompassGps() {

	}

	public SingleCompassGps(Compass compass) {
		this.compass = compass;
	}

	protected void doStart() throws CompassGpsException {
		if (compass == null) {
			throw new IllegalArgumentException("Must set the compass property");
		}
		TransactionIsolation defaultIsolation = ((LuceneSearchEngineFactory) ((InternalCompass) compass)
				.getSearchEngineFactory()).getLuceneSettings().getTransactionIsolation();
		if (defaultIsolation == TransactionIsolation.BATCH_INSERT) {
			throw new IllegalArgumentException(
					"The compass instance is configured with transaction isolation of batch_insert"
							+ ", there is no need since this CompassGps will execute the index operation with batch_index automatically, "
							+ " and mirroring with the configured transaction isolation");
		}
		indexCompassSettings = new CompassSettings();
		if (indexSettings == null) {
			indexCompassSettings.setSetting(CompassEnvironment.CONNECTION_SUB_CONTEXT, "gpsindex");
		} else {
			indexCompassSettings.addSettings(indexSettings);
		}
		this.compassTemplate = new CompassTemplate(compass);
	}

	protected void doStop() throws CompassGpsException {
	}

	protected void doIndex() throws CompassGpsException {
		boolean stoppedCompassOptimizer = false;
		if (compass.getSearchEngineOptimizer().isRunning()) {
			compass.getSearchEngineOptimizer().stop();
			stoppedCompassOptimizer = true;
		}
		// create the temp compass index, and clean it
		indexCompass = compass.clone(indexCompassSettings);
		indexCompass.getSearchEngineIndexManager().deleteIndex();
		indexCompass.getSearchEngineIndexManager().createIndex();
		indexCompassTemplate = new CompassTemplate(indexCompass);

		indexCompass.getSearchEngineIndexManager().clearCache();
		compass.getSearchEngineIndexManager().replaceIndex(indexCompass.getSearchEngineIndexManager(),
				new SearchEngineIndexManager.ReplaceIndexCallback() {
					public void buildIndexIfNeeded() throws SearchEngineException {
						for (Iterator it = devices.values().iterator(); it.hasNext();) {
							CompassGpsDevice device = (CompassGpsDevice) it.next();
							device.index();
						}
					}
				});

		indexCompass.getSearchEngineIndexManager().deleteIndex();
		indexCompass.close();
		indexCompass = null;
		indexCompassTemplate = null;

		if (stoppedCompassOptimizer) {
			compass.getSearchEngineOptimizer().start();
		}
	}

	public void executeForIndex(CompassCallback callback) throws CompassException {
		if (indexCompassTemplate == null) {
			compassTemplate.execute(indexTransactionIsolation, callback);
			return;
		}
		indexCompassTemplate.execute(indexTransactionIsolation, callback);
	}

	public void executeForMirror(CompassCallback callback) throws CompassException {
		compassTemplate.execute(callback);
	}

	public boolean hasMappingForEntityForIndex(Class clazz) throws CompassException {
		return hasMappingForEntity(clazz, getIndexCompass());
	}

	public boolean hasMappingForEntityForIndex(String name) throws CompassException {
		return hasMappingForEntity(name, getIndexCompass());
	}

	public boolean hasMappingForEntityForMirror(Class clazz) throws CompassException {
		return hasMappingForEntity(clazz, compass);
	}

	public boolean hasMappingForEntityForMirror(String name) throws CompassException {
		return hasMappingForEntity(name, compass);
	}

	public Compass getIndexCompass() {
		if (indexCompass == null) {
			return compass;
		}
		return indexCompass;
	}

	public Compass getMirrorCompass() {
		return compass;
	}

	/**
	 * Sets the compass instance that will be used with this Gps implementation.
	 * It will be used directly for mirror operations, and will be cloned
	 * (optionally adding the {@link #setIndexSettings(java.util.Properties)}
	 * for index operations.
	 */
	public void setCompass(Compass compass) {
		this.compass = compass;
	}

	/**
	 * Sets the transaction isolation for the clones compass used for the index
	 * process.
	 */
	public void setIndexTransactionIsolation(CompassTransaction.TransactionIsolation indexTransactionIsolation) {
		this.indexTransactionIsolation = indexTransactionIsolation;
	}

	/**
	 * Sets the additional cloned compass index settings. The settings can
	 * override existing settings used to create the Compass instance. Can be
	 * used to define different connection string for example.
	 */
	public void setIndexSettings(Properties indexSettings) {
		this.indexSettings = indexSettings;
	}
}
