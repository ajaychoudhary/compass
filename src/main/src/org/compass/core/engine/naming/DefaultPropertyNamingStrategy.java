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

package org.compass.core.engine.naming;

/**
 * A default implementation of the property naming strategy. Hidden properties
 * tart with the '$' sign. And the path is constructed usign the '/' sign.
 * 
 * @see org.compass.core.engine.naming.PropertyNamingStrategy
 * @see org.compass.core.engine.naming.DefaultPropertyNamingStrategyFactory
 * @author kimchy
 */
public class DefaultPropertyNamingStrategy implements PropertyNamingStrategy {

    public boolean isInternal(String name) {
        return name.charAt(0) == '$';
    }

    public String getRootPath() {
        return "$";
    }

    public String buildPath(String root, String name) {
        return root + "/" + name;
    }
}
