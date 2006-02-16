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

package org.compass.core.mapping;

import java.util.Enumeration;
import java.util.Properties;

import org.compass.core.converter.Converter;

/**
 * @author kimchy
 */
public abstract class AbstractMapping implements Mapping {

    private String name;
    
    private String path;

    private String converterName;
    
    private Converter converter;

    private Properties converterParameters = new Properties();

    protected void copy(Mapping copy) {
        copy.setConverter(getConverter());
        Enumeration propertiesNames = converterParameters.propertyNames();
        while (propertiesNames.hasMoreElements()) {
            String paramName = propertiesNames.nextElement().toString();
            copy.addConverterParam(paramName, converterParameters.getProperty(paramName));
        }
        copy.setName(getName());
        copy.setPath(getPath());
        copy.setConverterName(getConverterName());
    }

    public void addConverterParam(String paramName, String paramValue) {
        converterParameters.setProperty(paramName, paramValue);
    }

    public String getConverterParam() {
        return converterParameters.getProperty(Mapping.DEFAULT_PARAM);
    }

    public String getConverterParam(String paramName) {
        return converterParameters.getProperty(paramName);
    }

    public boolean controlsObjectNullability() {
        return true;
    }

    public Converter getConverter() {
        return converter;
    }

    public void setConverter(Converter converter) {
        this.converter = converter;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getConverterName() {
        return converterName;
    }

    public void setConverterName(String converterName) {
        this.converterName = converterName;
    }
}
