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

package org.compass.core.util;

import org.compass.core.Property;
import org.compass.core.Resource;
import org.compass.core.engine.SearchEngine;
import org.compass.core.engine.SearchEngineException;
import org.compass.core.mapping.CompassMapping;
import org.compass.core.mapping.ResourceMapping;
import org.compass.core.mapping.ResourcePropertyMapping;

/**
 * @author kimchy
 */
public abstract class ResourceHelper {

    private ResourceHelper() {
    }

    public static Property[] toIds(String alias, Resource resource, CompassMapping mapping)
            throws SearchEngineException {
        ResourceMapping resourceMapping = mapping.getRootMappingByAlias(alias);
        if (resourceMapping == null) {
            throw new SearchEngineException("Failed to find mappings for alias [" + alias + "]");
        }
        ResourcePropertyMapping[] pMappings = resourceMapping.getIdMappings();
        Property[] ids = new Property[pMappings.length];
        for (int i = 0; i < pMappings.length; i++) {
            ids[i] = resource.getProperty(pMappings[i].getPath());
            if (ids[i] == null) {
                throw new SearchEngineException("Id with path [" + pMappings[i].getPath() + "] for alias [" + alias
                        + "] not found");
            }
            if (!ids[i].isIndexed() || ids[i].isTokenized() || !ids[i].isStored()) {
                throw new SearchEngineException("Id [" + ids[i].getName() + "] for alias [" + alias
                        + "] must be stored and un_tokenized");
            }
        }
        return ids;
    }

    public static Property[] toIds(SearchEngine searchEngine, String[] values, ResourceMapping mapping)
            throws SearchEngineException {
        ResourcePropertyMapping[] idsMapping = mapping.getIdMappings();
        if (values.length != idsMapping.length) {
            throw new SearchEngineException("The id values don't match the id mapping. Id values ["
                    + StringUtils.arrayToCommaDelimitedString(values) + "], Ids Mappings ["
                    + StringUtils.arrayToCommaDelimitedString(idsMapping) + "]");
        }
        Property[] properties = new Property[values.length];
        for (int i = 0; i < values.length; i++) {
            String name = idsMapping[i].getPath();
            properties[i] = searchEngine.createProperty(name, values[i], Property.Store.YES, Property.Index.UN_TOKENIZED);
        }
        return properties;
    }

}
