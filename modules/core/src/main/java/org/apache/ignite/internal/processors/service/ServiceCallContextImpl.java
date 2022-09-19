/*
 * Copyright 2022 GridGain Systems, Inc. and Contributors.
 *
 * Licensed under the GridGain Community Edition License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gridgain.com/products/software/community-edition/gridgain-community-edition-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.service;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.services.ServiceCallContext;

/**
 * Service call context implementation.
 */
public class ServiceCallContextImpl implements ServiceCallContext {
    /** */
    private static final long serialVersionUID = 0L;

    /** Service call context attributes. */
    private Map<String, Object> attrs;

    /**
     * Default contructor.
     */
    public ServiceCallContextImpl() {
        attrs = new HashMap<>();
    }

    /**
     * @param attrs Service call context attributes.
     */
    public ServiceCallContextImpl(Map<String, Object> attrs) {
        this.attrs = attrs;
    }

    /** {@inheritDoc} */
    @Override public String attribute(String name) {
        return (String)attrs.get(name);
    }

    /** {@inheritDoc} */
    @Override public byte[] binaryAttribute(String name) {
        return (byte[])attrs.get(name);
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        U.writeMap(out, attrs);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        attrs = U.readMap(in);
    }

    /**
     * @return Service call context attributes.
     */
    Map<String, Object> values() {
        return attrs;
    }
}
