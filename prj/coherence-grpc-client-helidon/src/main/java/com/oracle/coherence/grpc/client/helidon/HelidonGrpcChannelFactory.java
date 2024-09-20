/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client.helidon;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.net.InetSocketAddress32;

import com.oracle.coherence.grpc.client.common.AbstractGrpcChannelFactory;
import com.oracle.coherence.grpc.client.common.GrpcChannelFactory;
import com.oracle.coherence.grpc.client.common.GrpcRemoteService;

import com.tangosol.coherence.config.builder.SocketProviderBuilder;

import com.tangosol.internal.net.grpc.RemoteGrpcServiceDependencies;

import com.tangosol.net.OperationalContext;

import com.tangosol.net.grpc.GrpcChannelDependencies;

import com.tangosol.net.grpc.GrpcDependencies;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ChannelCredentials;
import io.grpc.ClientCall;
import io.grpc.EquivalentAddressGroup;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.NameResolver;
import io.grpc.Status;

import io.helidon.common.tls.TlsConfig;

import io.helidon.http.HeaderNames;

import io.helidon.webclient.grpc.GrpcClient;
import io.helidon.webclient.grpc.GrpcClientConfig;
import io.helidon.webclient.grpc.GrpcClientProtocolConfig;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.UnknownHostException;

import java.time.Duration;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link GrpcChannelFactory} to create a Helidon gRPC channel.
 */
public class HelidonGrpcChannelFactory
        extends AbstractGrpcChannelFactory
    {
    /**
     * Default constructor required to instantiate this class
     * using the Java service loader.
     */
    public HelidonGrpcChannelFactory()
        {
        }

    @Override
    public int getPriority()
        {
        // must be higher priority than the default Netty client
        return GrpcChannelFactory.PRIORITY_NORMAL + 1;
        }

    @Override
    public Channel getChannel(GrpcRemoteService<?> service)
        {
        RemoteGrpcServiceDependencies depsService    = service.getDependencies();
        OperationalContext            ctx            = (OperationalContext) service.getCluster();
        String                        sService       = service.getServiceName();
        String                        sKey           = GrpcServiceInfo.createKey(service);
        String                        sRemoteService = depsService.getRemoteServiceName();
        String                        sRemoteCluster = depsService.getRemoteClusterName();
        GrpcChannelDependencies       depsChannel    = depsService.getChannelDependencies();
        GrpcServiceInfo               info           = new GrpcServiceInfo(ctx, sService, sRemoteService, sRemoteCluster, depsChannel);

        long nDeadline = depsService.getDeadline();
        if (nDeadline <= 0)
            {
            nDeadline = GrpcDependencies.DEFAULT_DEADLINE_MILLIS;
            }

        m_mapServiceInfo.put(sKey, info);

        AddressProviderNameResolver resolver = new AddressProviderNameResolver(depsChannel, info, null);
        ChannelWrapper              wrapper  = new ChannelWrapper(resolver, nDeadline);

        HelidonCredentialsHelper.createTlsConfig(depsChannel.getSocketProviderBuilder())
                .ifPresent(wrapper::tlsConfig);

        depsChannel.getAuthorityOverride().ifPresent(wrapper::overrideAuthority);

        return wrapper;
        }

    @Override
    protected ChannelCredentials createChannelCredentials(SocketProviderBuilder builder)
        {
        return null;
        }

    // ----- inner class: ChannelWrapper ------------------------------------

    /**
     * A {@link Channel} implementation that creates a real Helidon channel
     * on demand. This allows lazy name service lookups for the gRPC proxy
     * endpoints. This code re-uses some of the {@link NameResolver} code
     * from the base class, even though Helidon does not really support
     * name resolution.
     */
    protected static class ChannelWrapper
            extends Channel
        {
        public ChannelWrapper(AddressProviderNameResolver resolver, long nDeadline)
            {
            m_listener  = new Listener();
            m_resolver  = resolver;
            m_nDeadline = nDeadline;
            }

        @Override
        public <RequestT, ResponseT> ClientCall<RequestT, ResponseT>
                newCall(MethodDescriptor<RequestT, ResponseT> descriptor, CallOptions options)
            {
            ClientCall<RequestT, ResponseT> call = ensureChannel().newCall(descriptor, options);
            return new ForwardingClientCall<>()
                {
                @Override
                public void start(Listener<ResponseT> listener, Metadata headers)
                    {
                    Listener<ResponseT> forwardingListener = new ForwardingClientCallListener<>()
                        {
                        @Override
                        protected Listener<ResponseT> delegate()
                            {
                            return listener;
                            }

                        @Override
                        public void onClose(Status status, Metadata trailers)
                            {
                            super.onClose(status, trailers);
                            Channel channel = m_channel;
                            f_lock.lock();
                            try
                                {
                                if (channel != null && channel == m_channel)
                                    {
                                    m_channel = null;
                                    }
                                }
                            finally
                                {
                                f_lock.unlock();
                                }
                            }
                        };
                    super.start(forwardingListener, headers);
                    }

                @Override
                protected ClientCall<RequestT, ResponseT> delegate()
                    {
                    return call;
                    }
                };
            }

        @Override
        public String authority()
            {
            return m_sAuthority == null ? ensureChannel().authority() : m_sAuthority;
            }

        public void overrideAuthority(String sAuthority)
            {
            m_sAuthority = sAuthority;
            }

        public Listener getListener()
            {
            return m_listener;
            }

        // ----- helper methods ---------------------------------------------

        protected Channel ensureChannel()
            {
            Channel channel = m_channel;
            if (m_channel == null)
                {
                f_lock.lock();
                try
                    {
                    channel = m_channel;
                    if (channel == null)
                        {
                        m_resolver.start(m_listener);
                        URI uri = m_listAddress.stream()
                                .map(this::toURI)
                                .filter(Objects::nonNull)
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException("No remote addresses are available"));

                        GrpcClientProtocolConfig config = GrpcClientProtocolConfig.builder()
                                .pollWaitTime(Duration.ofSeconds(10))
                                .heartbeatPeriod(Duration.ofSeconds(5))
                                .abortPollTimeExpired(false)
                                .build();

                        GrpcClientConfig.Builder builder = GrpcClient.builder()
                                .protocolConfig(config)
                                .baseUri(uri)
                                .addHeader(HeaderNames.USER_AGENT, "Coherence Java Client");

                        if (m_tlsConfig != null)
                            {
                            builder.tls(m_tlsConfig);
                            }

                        GrpcClient client = builder.build();
                        channel = m_channel = client.channel();
                        }
                    }
                finally
                    {
                    f_lock.unlock();
                    }
                }
            return channel;
            }

        protected URI toURI(EquivalentAddressGroup eag)
            {
            return eag.getAddresses().stream()
                .map(this::toURI)
                .findFirst()
                .orElse(null);
            }

        protected URI toURI(SocketAddress address)
            {
            String sHost;
            int    nPort;
            if (address instanceof InetSocketAddress)
                {
                sHost = ((InetSocketAddress) address).getHostName();
                nPort = ((InetSocketAddress) address).getPort();
                }
            else if (address instanceof InetSocketAddress32)
                {
                sHost = ((InetSocketAddress32) address).getHostName();
                nPort = ((InetSocketAddress32) address).getPort();
                }
            else
                {
                throw new IllegalArgumentException("Invalid socket address type: " + address);
                }
            if ("localhost".equalsIgnoreCase(sHost))
                {
                try
                    {
                    sHost = InetAddress.getLocalHost().getHostName();
                    }
                catch (UnknownHostException e)
                    {
                    // ignored
                    }
                }
            return URI.create(m_sScheme + "://" + sHost + ":" + nPort);
            }

        protected void tlsConfig(TlsConfig tlsConfig)
            {
            m_tlsConfig = tlsConfig;
            m_sScheme   = tlsConfig == null ? "http" : "https";
            }

        // ----- inner class: Listener --------------------------------------

        /**
         * A basic {@link NameResolver.Listener2} that receives the endpoints
         * resolved from the Coherence name service.
         */
        protected class Listener
                extends NameResolver.Listener2
            {
            @Override
            public void onResult(NameResolver.ResolutionResult result)
                {
                f_lock.lock();
                try
                    {
                    m_listAddress = result.getAddresses();
                    }
                finally
                    {
                    f_lock.unlock();
                    }
                }

            @Override
            public void onError(Status error)
                {
                Logger.err("Error resolving gRPC endpoints due to: " + error);
                }
            }

        // ----- data members -----------------------------------------------

        /**
         * The endpoint address resolver.
         */
        private final AddressProviderNameResolver m_resolver;

        /**
         * The {@link Listener} to receive name service lookup results.
         */
        private final Listener m_listener;

        /**
         * The authority for the channel URI.
         */
        private String m_sAuthority;

        /**
         * The channel TLS configuration.
         */
        private TlsConfig m_tlsConfig;

        /**
         * The channel scheme.
         */
        private String m_sScheme = "http";

        /**
         * The real gRPC channel.
         */
        private Channel m_channel;

        /**
         * The lock to control thread safety.
         */
        private final ReentrantLock f_lock = new ReentrantLock();

        /**
         * The list of addresses resolved by the name service.
         */
        private List<EquivalentAddressGroup> m_listAddress = Collections.emptyList();

        private long m_nDeadline = GrpcDependencies.DEFAULT_DEADLINE_MILLIS;
        }
    }
