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

package org.compass.core.config;

import java.util.*;

import org.compass.core.util.ClassUtils;

/**
 * A set of settings that are used to configure the Compass instance.
 *
 * @author kimchy
 */
public class CompassSettings {

    private Properties settings;

    private HashMap groups = new HashMap();

    public CompassSettings() {
        this(new Properties());
    }

    public CompassSettings(Properties settings) {
        this.settings = settings;
    }

    public void addSettings(Properties settings) {
        this.settings.putAll(settings);
    }

    public void addSettings(CompassSettings settings) {
        this.settings.putAll(settings.getProperties());
    }

    public CompassSettings copy() {
        return new CompassSettings((Properties) settings.clone());
    }

    public Properties getProperties() {
        return settings;
    }

    public Collection keySet() {
        return settings.keySet();
    }

    public String getSetting(String setting) {
        return settings.getProperty(setting);
    }

    public String getSetting(String setting, String defaultValue) {
        return settings.getProperty(setting, defaultValue);
    }

    public Map getSettingGroups(String settingPrefix) {
        if (settingPrefix.charAt(settingPrefix.length() - 1) != '.') {
            settingPrefix = settingPrefix + ".";
        }
        Map group = (Map) groups.get(settingPrefix);
        if (group != null) {
            return group;
        }
        synchronized (groups) {
            HashMap map = new HashMap();
            for (Iterator it = settings.keySet().iterator(); it.hasNext();) {
                String setting = (String) it.next();
                if (setting.startsWith(settingPrefix)) {
                    String nameValue = setting.substring(settingPrefix.length());
                    int dotIndex = nameValue.indexOf('.');
                    if (dotIndex == -1) {
                        throw new ConfigurationException("Failed to get setting group for [" + settingPrefix
                                + "] setting prefix and setting [" + setting + "] because of a missing '.'");
                    }
                    String name = nameValue.substring(0, dotIndex);
                    String value = nameValue.substring(dotIndex + 1);
                    CompassSettings groupSettings = (CompassSettings) map.get(name);
                    if (groupSettings == null) {
                        groupSettings = new CompassSettings();
                        map.put(name, groupSettings);
                    }
                    groupSettings.setSetting(value, getSetting(setting));
                }
            }
            groups.put(settingPrefix, map);
            return map;
        }
    }

    public float getSettingAsFloat(String setting, float defaultValue) {
        String sValue = getSetting(setting);
        if (sValue == null) {
            return defaultValue;
        }
        return Float.parseFloat(sValue);
    }

    public int getSettingAsInt(String setting, int defaultValue) {
        String sValue = getSetting(setting);
        if (sValue == null) {
            return defaultValue;
        }
        return Integer.parseInt(sValue);
    }

    public long getSettingAsLong(String setting, long defaultValue) {
        String sValue = getSetting(setting);
        if (sValue == null) {
            return defaultValue;
        }
        return Long.parseLong(sValue);
    }

    public boolean getSettingAsBoolean(String setting, boolean defaultValue) {
        String sValue = getSetting(setting);
        if (sValue == null) {
            return defaultValue;
        }
        return Boolean.valueOf(sValue).booleanValue();
    }

    public Class getSettingAsClass(String setting, Class clazz) throws ClassNotFoundException {
        String sValue = getSetting(setting);
        if (sValue == null) {
            return clazz;
        }
        return ClassUtils.forName(sValue);
    }

    public Class getSettingAsClass(String setting, Class clazz, ClassLoader classLoader) throws ClassNotFoundException {
        String sValue = getSetting(setting);
        if (sValue == null) {
            return clazz;
        }
        return ClassUtils.forName(sValue, classLoader);
    }

    public CompassSettings setSetting(String setting, String value) {
        if (value == null) {
            return this;
        }
        this.settings.setProperty(setting, value);
        return this;
    }

    public CompassSettings setBooleanSetting(String setting, boolean value) {
        setSetting(setting, String.valueOf(value));
        return this;
    }

    public CompassSettings setFloatSetting(String setting, float value) {
        setSetting(setting, String.valueOf(value));
        return this;
    }

    public CompassSettings setIntSetting(String setting, int value) {
        setSetting(setting, String.valueOf(value));
        return this;
    }

    public CompassSettings setLongSetting(String setting, long value) {
        setSetting(setting, String.valueOf(value));
        return this;
    }

    public CompassSettings setClassSetting(String setting, Class clazz) {
        setSetting(setting, clazz.getName());
        return this;
    }

    /**
     * Sets a group of settings, sharing the same setting prefix. The provided
     * settings are appended to the settingPrefix, and the matching values are
     * set.
     * <p/>
     * The constructed setting is: settingPrefix + "." + groupName + "." +
     * settings[i].
     */
    public CompassSettings setGroupSettings(String settingPrefix, String groupName, String[] settings, String[] values) {
        if (settings.length != values.length) {
            throw new IllegalArgumentException("The settings length must match the value length");
        }
        for (int i = 0; i < settings.length; i++) {
            if (values[i] == null) {
                continue;
            }
            setSetting(settingPrefix + "." + groupName + "." + settings[i], values[i]);
        }
        return this;
    }

    public String toString() {
        return settings.toString();
    }
}
