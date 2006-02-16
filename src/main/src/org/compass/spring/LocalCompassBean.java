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

package org.compass.spring;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.compass.core.Compass;
import org.compass.core.CompassException;
import org.compass.core.config.CompassConfiguration;
import org.compass.core.config.CompassEnvironment;
import org.compass.core.config.InputStreamMappingResolver;
import org.compass.core.converter.Converter;
import org.compass.core.impl.InternalCompass;
import org.compass.core.lucene.LuceneEnvironment;
import org.compass.core.lucene.engine.store.jdbc.ExternalDataSourceProvider;
import org.compass.spring.transaction.SpringSyncTransactionFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author kimchy
 */
public class LocalCompassBean implements FactoryBean, InitializingBean, DisposableBean {

    protected static final Log log = LogFactory.getLog(LocalCompassBean.class);

    private Resource configLocation;

    private Resource[] resourceLocations;

    private Resource[] resourceJarLocations;

    private Resource[] resourceDirectoryLocations;

    private InputStreamMappingResolver[] mappingResolvers;

    private Properties compassSettings;

    private DataSource dataSource;

    private PlatformTransactionManager transactionManager;

    private Map convertersByName;

    private Compass compass;

    /**
     * Set the location of the Compass XML config file, for example as classpath
     * resource "classpath:compass.cfg.xml".
     * <p/>
     * Note: Can be omitted when all necessary properties and mapping resources
     * are specified locally via this bean.
     */
    public void setConfigLocation(Resource configLocation) {
        this.configLocation = configLocation;
    }

    public void setCompassSettings(Properties compassSettings) {
        this.compassSettings = compassSettings;
    }

    /**
     * Set locations of Compass resource files (mapping and common metadata),
     * for example as classpath resource "classpath:example.cpm.xml". Supports
     * any resource location via Spring's resource abstraction, for example
     * relative paths like "WEB-INF/mappings/example.hbm.xml" when running in an
     * application context.
     * <p/>
     * Can be used to add to mappings from a Compass XML config file, or to
     * specify all mappings locally.
     */
    public void setResourceLocations(Resource[] resourceLocations) {
        this.resourceLocations = resourceLocations;
    }

    /**
     * Set locations of jar files that contain Compass resources, like
     * "WEB-INF/lib/example.jar".
     * <p/>
     * Can be used to add to mappings from a Compass XML config file, or to
     * specify all mappings locally.
     */
    public void setResourceJarLocations(Resource[] resourceJarLocations) {
        this.resourceJarLocations = resourceJarLocations;
    }

    /**
     * Set locations of directories that contain Compass mapping resources, like
     * "WEB-INF/mappings".
     * <p/>
     * Can be used to add to mappings from a Compass XML config file, or to
     * specify all mappings locally.
     */
    public void setResourceDirectoryLocations(Resource[] resourceDirectoryLocations) {
        this.resourceDirectoryLocations = resourceDirectoryLocations;
    }

    /**
     * Sets the mapping resolvers the resolved Compass mapping definitions.
     */
    public void setMappingResolvers(InputStreamMappingResolver[] mappingResolvers) {
        this.mappingResolvers = mappingResolvers;
    }

    /**
     * Sets a <code>DataSource</code> to be used when the index is stored within a database.
     * The data source must be used with {@link org.compass.core.lucene.engine.store.jdbc.ExternalDataSourceProvider}
     * for externally configured data sources (such is the case some of the time with spring). If set, Compass data source provider
     * does not have to be set, since it will automatically default to <code>ExternalDataSourceProvider</code>. If the
     * compass data source provider is set as a compass setting, it will be used.
     * <p/>
     * Note, that it will be automatically wrapped with Spring's <literal>TransactionAwareDataSourceProxy</literal> if not
     * already wrapped by one.
     * {@link org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy}.
     */
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        if (!(dataSource instanceof TransactionAwareDataSourceProxy)) {
            this.dataSource = new TransactionAwareDataSourceProxy(dataSource);
        }
    }

    /**
     * Sets Spring <code>PlatformTransactionManager</code> to be used with compass. If using
     * {@link org.compass.spring.transaction.SpringSyncTransactionFactory}, it must be set.
     */
    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /**
     * Sets a map of global converters to be registered with compass. The map key will be
     * the name that the converter will be registered against, and the value should be the
     * Converter itself (natuarally configured using spring DI).
     */
    public void setConvertersByName(Map convertersByName) {
        this.convertersByName = convertersByName;
    }

    public void afterPropertiesSet() throws Exception {
        CompassConfiguration config = newConfiguration();

        if (this.configLocation != null) {
            config.configure(this.configLocation.getURL());
        }

        if (this.compassSettings != null) {
            config.getSettings().addSettings(this.compassSettings);
        }

        if (resourceLocations != null) {
            for (int i = 0; i < resourceLocations.length; i++) {
                config.addInputStream(resourceLocations[i].getInputStream(), resourceLocations[i].getDescription());
            }
        }

        if (resourceJarLocations != null) {
            for (int i = 0; i < resourceJarLocations.length; i++) {
                config.addJar(resourceJarLocations[i].getFile());
            }
        }

        if (resourceDirectoryLocations != null) {
            for (int i = 0; i < resourceDirectoryLocations.length; i++) {
                File file = resourceDirectoryLocations[i].getFile();
                if (!file.isDirectory()) {
                    throw new IllegalArgumentException("Resource directory location ["
                            + this.resourceDirectoryLocations[i] + "] does not denote a directory");
                }
                config.addDirectory(file);
            }
        }

        if (mappingResolvers != null) {
            for (int i = 0; i < mappingResolvers.length; i++) {
                config.addMappingResover(mappingResolvers[i]);
            }
        }

        if (dataSource != null) {
            ExternalDataSourceProvider.setDataSource(dataSource);
            if (config.getSettings().getSetting(LuceneEnvironment.JdbcStore.DataSourceProvider.CLASS) == null) {
                config.getSettings().setSetting(LuceneEnvironment.JdbcStore.DataSourceProvider.CLASS,
                        ExternalDataSourceProvider.class.getName());
            }
        }

        String compassTransactionFactory = config.getSettings().getSetting(CompassEnvironment.Transaction.FACTORY);
        if (compassTransactionFactory != null && compassTransactionFactory.equals(SpringSyncTransactionFactory.class.getName()))
        {
            if (transactionManager == null) {
                throw new IllegalArgumentException("When using SpringSyncTransactionFactory the transactionManager property must be set");
            }
        }
        SpringSyncTransactionFactory.setTransactionManager(transactionManager);

        if (convertersByName != null) {
            for (Iterator it = convertersByName.keySet().iterator(); it.hasNext();) {
                String converterName = (String) it.next();
                config.registerConverter(converterName, (Converter) convertersByName.get(converterName));
            }
        }

        log.info("Building new Compass");
        this.compass = newCompass(config);
        this.compass = (Compass) Proxy.newProxyInstance(SpringCompassInvocationHandler.class.getClassLoader(),
                new Class[]{InternalCompass.class}, new SpringCompassInvocationHandler(this.compass));
    }

    protected CompassConfiguration newConfiguration() {
        return new CompassConfiguration();
    }

    protected Compass newCompass(CompassConfiguration config) throws CompassException {
        return config.buildCompass();
    }

    public Object getObject() throws Exception {
        return this.compass;
    }

    public Class getObjectType() {
        return (compass != null) ? compass.getClass() : Compass.class;
    }

    public boolean isSingleton() {
        return true;
    }

    public void destroy() throws Exception {
        this.compass.close();
    }

    /**
     * Invocation handler that suppresses close calls on JDBC Connections.
     */
    private class SpringCompassInvocationHandler implements InvocationHandler {

        private static final String GET_TARGET_COMPASS_METHOD_NAME = "getTargetCompass";

        private static final String CLONE_METHOD = "clone";

        private Compass targetCompass;

        public SpringCompassInvocationHandler(Compass targetCompass) {
            this.targetCompass = targetCompass;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // Invocation on ConnectionProxy interface coming in...

            if (method.getName().equals(GET_TARGET_COMPASS_METHOD_NAME)) {
                return compass;
            }

            if (method.getName().equals(CLONE_METHOD) && args.length == 1) {
                if (dataSource != null) {
                    ExternalDataSourceProvider.setDataSource(dataSource);
                }

                SpringSyncTransactionFactory.setTransactionManager(transactionManager);
            }

            // Invoke method on target connection.
            try {
                return method.invoke(targetCompass, args);
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }
    }

}
