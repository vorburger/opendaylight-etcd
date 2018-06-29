/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.dom2kv.test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Consumer which stores everything in a List.
 *
 * @author Michael Vorburger.ch
 */
public class ListConsumer<T> implements Consumer<T> {

    private final List<T> list = new ArrayList<>();

    @Override
    public void accept(T element) {
        list.add(element);
    }

    public List<T> getList() {
        return list;
    }
}
