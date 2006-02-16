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

package org.compass.core.converter.mapping.osem;

import java.lang.reflect.Array;

import org.compass.core.Resource;
import org.compass.core.mapping.Mapping;
import org.compass.core.mapping.osem.AbstractCollectionMapping;
import org.compass.core.marshall.MarshallingContext;

/**
 * @author kimchy
 */
public class ArrayMappingConverter extends AbstractCollectionMappingConverter {

    protected String getColClass(Object root) {
        return root.getClass().getComponentType().getName();
    }

    protected void marshallIterateData(Object root, AbstractCollectionMapping colMapping, Resource resource,
            MarshallingContext context) {
        int size = Array.getLength(root);
        Mapping elementMapping = colMapping.getElementMapping();
        for (int i = 0; i < size; i++) {
            Object value = Array.get(root, i);
            elementMapping.getConverter().marshall(resource, value, elementMapping, context);
        }
    }

    protected int getActualSize(Object root, AbstractCollectionMapping colMapping, Resource resource, MarshallingContext context) {
        int size = Array.getLength(root);
        int actualSize = 0;
        for (int i = 0; i < size; i++) {
            Object value = Array.get(root, i);
            if (value != null) {
                actualSize++;
            }
        }
        return actualSize;
    }

    protected Object createColObject(Class colClass, int size) {
        return Array.newInstance(colClass, size);
    }

    protected void addValue(Object col, int index, Object value) {
        Array.set(col, index, value);
    }
}
