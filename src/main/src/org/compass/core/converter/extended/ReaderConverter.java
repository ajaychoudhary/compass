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

package org.compass.core.converter.extended;

import java.io.Reader;

import org.compass.core.Property;
import org.compass.core.Resource;
import org.compass.core.converter.ConversionException;
import org.compass.core.converter.Converter;
import org.compass.core.engine.SearchEngine;
import org.compass.core.mapping.Mapping;
import org.compass.core.mapping.ResourcePropertyMapping;
import org.compass.core.marshall.MarshallingContext;

/**
 * @author kimchy
 */
public class ReaderConverter implements Converter {

    public void marshall(Resource resource, Object root, Mapping mapping, MarshallingContext context)
            throws ConversionException {
        ResourcePropertyMapping resourcePropertyMapping = (ResourcePropertyMapping) mapping;
        SearchEngine searchEngine = context.getSearchEngine();

        // don't save a null value
        if (root == null) {
            return;
        }

        String propertyName = resourcePropertyMapping.getPath();
        Reader value = (Reader) root;
        Property p = searchEngine.createProperty(propertyName, value, resourcePropertyMapping.getTermVector());
        p.setBoost(resourcePropertyMapping.getBoost());
        resource.addProperty(p);
    }

    public Object unmarshall(Resource resource, Mapping mapping, MarshallingContext context) throws ConversionException {
        // no value for Reader types, since they are not stored.
        return null;
    }
}
