/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client.common;

/**
 * A listener that is called when a resource is deactivated.
 *
 * @param <T> the type of the resource
 *
 * @author Jonathan Knight  2019.11.29
 * @since 20.06
 */
public interface DeactivationListener<T>
    {
    /**
     * Called to indicate that a resource was released.
     *
     * @param resource the resource that was released
     */
    void released(T resource);

    /**
     * Called to indicate that a resource was destroyed.
     *
     * @param resource the resource that was destroyed
     */
    void destroyed(T resource);
    }
