/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.dom2kv.gson.test;

import java.util.Optional;
import java.util.Properties;
import java.util.function.BiConsumer;

/**
 * {@link BiConsumer} which stores elements as {@link Properties}. Typically for
 * tests & illustration, not production (because e.g. null handling isn't
 * correct and Properties has issues).
 *
 * @author Michael Vorburger.ch
 */
public class PropertiesBiConsumer implements BiConsumer<String, Optional<Object>> {

    private final Properties properties = new Properties();

    @Override
    public void accept(String key, Optional<Object> value) {
        properties.put(key, value.orElse(""));
    }

    public Properties getProperties() {
        return properties;
    }
}
