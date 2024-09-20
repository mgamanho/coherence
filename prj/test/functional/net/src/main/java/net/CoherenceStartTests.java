/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package net;

import com.oracle.bedrock.testsupport.MavenProjectFileUtils;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.Service;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.fail;

public class CoherenceStartTests
    {
    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void _startup()
        {
        File buildFolder   = MavenProjectFileUtils.locateBuildFolder(CoherenceStartTests.class);
        File classesFolder = new File(buildFolder, "classes");
        File fileGar       = new File(classesFolder, "coh_new_examples.gar");

        GAR_FILE_NAME = fileGar.getAbsolutePath();

        System.setProperty("coherence.distributed.localstorage", "true");

        s_scheduler = Executors.newScheduledThreadPool(1);
        }

    @After
    public void _postTestCleanup()
        {
        // if a previous test shuts down the cluster on a different thread,
        // we need to ensure the next test doesn't pick up a "disposed" cluster
        Coherence.closeAll();
        try
            {
            CacheFactory.shutdown();
            }
        catch (Exception ignored)
            {
            }
        try
            {
            DefaultCacheServer.shutdown();
            }
        catch (Exception ignored)
            {
            }
        }

    @Test
    public void shouldStartGarServer() throws Exception
        {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> Coherence.main(new String[]{GAR_FILE_NAME}));

        try
            {
            future.get(1, TimeUnit.MINUTES);
            fail("Should have thrown an exception");
            }
        catch (ExecutionException e)
            {
            System.out.println();
            }
        }

    @Test
    public void shouldStartWithSpecifiedCacheConfig()
        {
        s_scheduler.submit(
                () -> Coherence.main(new String[]{"test-cache-config.xml"}));

        Eventually.assertDeferred(Coherence::getInstance, is(notNullValue()));
        Coherence coherence = Coherence.getInstance();
        Eventually.assertDeferred(coherence::isStarted, is(true));

        Service service = CacheFactory.getService("TestService");
        assertThat(service, is(instanceOf(DistributedCacheService.class)));
        }

    @Test
    public void shouldStartWithIgnoredNumericArg()
        {
        s_scheduler.submit(
                () -> Coherence.main(new String[]{"1234"}));

        Eventually.assertDeferred(Coherence::getInstance, is(notNullValue()));
        Coherence coherence = Coherence.getInstance();
        Eventually.assertDeferred(coherence::isStarted, is(true));
        }

    // ----- data members ---------------------------------------------------

    /**
     * The GAR file to be exploded by the tests
     */
    public static String GAR_FILE_NAME;

    /**
     * Scheduler service commonly used to start or shutdown DCS in a
     * background thread.
     */
    private static ScheduledExecutorService s_scheduler;
    }
