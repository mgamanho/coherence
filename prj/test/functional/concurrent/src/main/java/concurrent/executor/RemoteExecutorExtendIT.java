/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.executor;

import com.oracle.bedrock.junit.CoherenceClusterExtension;

import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.options.ClusterPort;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.Multicast;
import com.oracle.bedrock.runtime.coherence.options.RoleName;

import com.oracle.bedrock.runtime.java.options.ClassName;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;

import com.oracle.bedrock.runtime.options.DisplayName;

import com.oracle.bedrock.runtime.options.StabilityPredicate;
import com.tangosol.net.Coherence;

import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests to ensure RemoteExecutor can be established over extend.
 *
 * @author rl   7.12.2021
 * @since 21.12
 */
public class RemoteExecutorExtendIT
        extends AbstractRemoteExecutorTest
    {
    // ----- AbstractRemoteExecutorTest methods -----------------------------

    @Override
    protected Coherence getClient()
        {
        return Coherence.client();
        }

    // ----- data members ---------------------------------------------------

    /**
     * A Bedrock utility to capture logs of spawned processes into files
     * under target/test-output. This is added as an option to the cluster
     * and client processes.
     */
    static TestLogs logs = new TestLogs(RemoteExecutorExtendIT.class);

    /**
     * A Bedrock JUnit5 extension that starts a Coherence cluster of a single
     * storage-enabled member.
     */
    @RegisterExtension
    static CoherenceClusterExtension coherenceResource =
            new CoherenceClusterExtension()
                    .using(LocalPlatform.get())
                    .with(ClassName.of(Coherence.class),
                          Logging.at(9),
                          LocalHost.only(),
                          Multicast.ttl(0),
                          IPv4Preferred.yes(),
                          logs,
                          ClusterPort.automatic(),
                          StabilityPredicate.of(CoherenceCluster.Predicates.isCoherenceRunning()))
                    .include(1,
                             DisplayName.of("storage"),
                             RoleName.of("storage"),
                             LocalStorage.enabled(),
                             Logging.at(9));
    }
