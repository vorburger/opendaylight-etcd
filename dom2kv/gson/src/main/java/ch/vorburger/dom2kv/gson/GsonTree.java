/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.dom2kv.gson;

import ch.vorburger.dom2kv.Tree;
import ch.vorburger.dom2kv.impl.MapTreeBuilder;
import ch.vorburger.dom2kv.impl.TreeBuilderImpl;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.util.HashMap;
import java.util.Map;

/**
 * Converter from GSON JSON to and from {@link Tree}.
 *
 * @author Michael Vorburger.ch
 */
public class GsonTree {

    MapTreeBuilder<String, Object> mapTreeBuilder = new MapTreeBuilder<>(() -> new TreeBuilderImpl<>());

    public Tree<String, Object> parse(String jsonText) {
        Gson gson = new Gson();
        Map<String, Object> map = gson.fromJson(jsonText, new TypeToken<HashMap<String, Object>>(){}.getType());
        if (map == null) {
            throw new JsonSyntaxException("Empty; no JSON.");
        }
        return mapTreeBuilder.fromMap(map);
    }
}
