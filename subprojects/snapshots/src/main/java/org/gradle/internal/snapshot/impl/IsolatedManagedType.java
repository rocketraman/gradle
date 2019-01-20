/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.internal.snapshot.impl;

import com.google.common.collect.ImmutableList;
import org.gradle.internal.Cast;
import org.gradle.internal.instantiation.HasManagedState;
import org.gradle.internal.isolation.Isolatable;
import org.gradle.internal.snapshot.ValueSnapshot;

import javax.annotation.Nullable;

public class IsolatedManagedType extends AbstractManagedTypeSnapshot<Isolatable<?>> implements Isolatable<Object> {
    private final HasManagedState.Factory factory;
    private final Class<?> targetType;

    public IsolatedManagedType(Class<?> targetType, HasManagedState.Factory factory, ImmutableList<Isolatable<?>> state) {
        super(state);
        this.targetType = targetType;
        this.factory = factory;
    }

    @Override
    public ValueSnapshot asSnapshot() {
        ImmutableList.Builder<ValueSnapshot> builder = ImmutableList.builderWithExpectedSize(state.size());
        for (Isolatable<?> element : state) {
            builder.add(element.asSnapshot());
        }
        return new ManagedTypeSnapshot(targetType.getName(), builder.build());
    }

    @Override
    public Object isolate() {
        Object[] params = new Object[state.size()];
        for (int i = 0; i < state.size(); i++) {
            Isolatable<?> isolatable = state.get(i);
            params[i] = isolatable.isolate();
        }
        return factory.fromState(targetType, params);
    }

    @Nullable
    @Override
    public <S> Isolatable<S> coerce(Class<S> type) {
        if (type.isAssignableFrom(targetType)) {
            return Cast.uncheckedCast(this);
        }
        if (targetType.getName().equals(type.getName())) {
            return Cast.uncheckedCast(new IsolatedManagedType(type, factory, state));
        }
        return null;
    }
}
