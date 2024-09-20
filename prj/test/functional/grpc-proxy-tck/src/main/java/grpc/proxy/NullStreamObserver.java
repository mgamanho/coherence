/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy;

import io.grpc.stub.StreamObserver;

/**
 * A null implementation of a {@link io.grpc.stub.StreamObserver} for
 * use in tests where we do not care what happens to this observer.
 *
 * @author Jonathan Knight  2020.01.07
 * @since 20.06
 */
public class NullStreamObserver<V>
        implements StreamObserver<V>
    {
    // ----- constructors ---------------------------------------------------

    private NullStreamObserver()
        {
        }

    // ----- StreamObserver interface ---------------------------------------

    @Override
    public void onNext(V v)
        {
        }

    @Override
    public void onError(Throwable throwable)
        {
        }

    @Override
    public void onCompleted()
        {
        }

    // ----- factory methods ------------------------------------------------

    public static <T> StreamObserver<T> instance()
        {
        return new NullStreamObserver<>();
        }
    }
