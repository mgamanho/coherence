/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import com.tangosol.net.NamedCache;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * A collection of functional tests for {@code Coherence*Extend} that use the
 * {@value VIEW_EXTEND_NEAR} cache.
 *
 * @author rlubke
 * @since 12.2.1.4
 */
public class ViewExtendNearTests
        extends AbstractExtendTests
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Default constructor.
     */
    public ViewExtendNearTests()
        {
        super(VIEW_EXTEND_NEAR);
        }


    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup()
        {
        startCacheServerWithProxy(ViewExtendNearTests.class.getName(), "extend", FILE_SERVER_CFG_CACHE);
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer(ViewExtendNearTests.class.getName());
        }

    // ----- test methods ---------------------------------------------------

    // synthetic events not relayed to view cache
    @Test
    public void testExpiry()
        {
        // no-op
        }

    @Test
    public void valuesLazyDeserialization()
        {
        // no-op
        }

    // ----- helper methods -------------------------------------------------

    @Override
    protected NamedCache getNamedCache(String sCacheName, ClassLoader loader)
        {
        NamedCache cache = super.getNamedCache(sCacheName, loader);

        // as this is a view, it won't be destroyed by the super call, so clear it before the next test.
        cache.clear();
        return cache;
        }
    }
