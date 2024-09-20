/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.client;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;
import com.tangosol.io.SerializerFactory;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.OperationalContext;

import com.tangosol.net.grpc.GrpcDependencies;
import com.tangosol.util.Base;
import com.tangosol.util.SimpleMapEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.extension.RegisterExtension;

import org.junit.jupiter.params.ParameterizedTest;

import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings("rawtypes")
class NamedCacheServiceEntrySetIT
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    static void setupBaseTest()
        {
        s_realCache = s_serverHelper.getSession().getCache(CACHE_NAME);
        }

    @BeforeEach
    void beforeEach()
        {
        s_realCache.clear();
        }

    private <K, V> NamedCache<K, V> createClient(String serializerName, Serializer serializer)
        {
        return s_serverHelper.createClient(GrpcDependencies.DEFAULT_SCOPE, CACHE_NAME, serializerName, serializer);
        }

    // ----- test methods ---------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldGetEntrySetOfEmptyCache(String sSerializerName, Serializer serializer)
        {
        s_realCache.clear();
        NamedCache<String, String> cache = createClient(sSerializerName, serializer);

        Set<Map.Entry<String, String>> entries = cache.entrySet();
        assertThat(entries, is(notNullValue()));
        assertThat(entries.isEmpty(), is(true));
        assertThat(entries.size(), is(0));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldGetEntrySetIteratorOfEmptyCache(String sSerializerName, Serializer serializer)
        {
        s_realCache.clear();
        NamedCache<String, String> cache = createClient(sSerializerName, serializer);

        Set<Map.Entry<String, String>> entries = cache.entrySet();
        assertThat(entries, is(notNullValue()));

        Iterator<Map.Entry<String, String>> iterator = entries.iterator();
        assertThat(iterator, is(notNullValue()));
        assertThat(iterator.hasNext(), is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldGetEntrySetOfPopulatedCache(String sSerializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(5);
        NamedCache<String, String> cache = createClient(sSerializerName, serializer);

        Set<Map.Entry<String, String>> entries = cache.entrySet();
        assertThat(entries, is(notNullValue()));
        assertThat(entries.isEmpty(), is(false));
        assertThat(entries.size(), is(s_realCache.size()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldGetEntrySetIteratorOfPopulatedCache(String sSerializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String> cache = createClient(sSerializerName, serializer);

        Set<Map.Entry<String, String>> entries = cache.entrySet();
        assertThat(entries, is(notNullValue()));

        Iterator<Map.Entry<String, String>> iterator = entries.iterator();
        assertThat(iterator, is(notNullValue()));
        assertThat(iterator.hasNext(), is(true));

        Set<Map.Entry<String, String>> set = new HashSet<>();
        int count = 0;
        while (iterator.hasNext())
            {
            set.add(iterator.next());
            count++;
            }

        assertThat(count, is(s_realCache.size()));
        assertThat(set, is(s_realCache.entrySet()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldRemoveFromEntrySetIteratorOfPopulatedCache(String sSerializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String> cache = createClient(sSerializerName, serializer);

        Set<Map.Entry<String, String>> entries = cache.entrySet();
        assertThat(entries, is(notNullValue()));

        Iterator<Map.Entry<String, String>> iterator = entries.iterator();
        assertThat(iterator, is(notNullValue()));
        assertThat(iterator.hasNext(), is(true));

        while (iterator.hasNext())
            {
            iterator.next();
            iterator.remove();
            }

        assertThat(s_realCache.isEmpty(), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldClearEntrySet(String sSerializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String> cache = createClient(sSerializerName, serializer);

        Set<Map.Entry<String, String>> entries = cache.entrySet();
        entries.clear();
        assertThat(s_realCache.isEmpty(), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldContainAllEntries(String sSerializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String> cache = createClient(sSerializerName, serializer);

        Set<Map.Entry<String, String>> entries = cache.entrySet();

        boolean result  = entries.containsAll(Arrays.asList(
                new SimpleMapEntry<>("key-1", "value-1"),
                new SimpleMapEntry<>("key-2", "value-2"),
                new SimpleMapEntry<>("key-3", "value-3")
        ));

        assertThat(result, is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldNotContainAllEntries(String sSerializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String> cache = createClient(sSerializerName, serializer);

        Set<Map.Entry<String, String>> entries = cache.entrySet();

        boolean result = entries.containsAll(Arrays.asList(
                new SimpleMapEntry<>("key-1", "value-1"),
                new SimpleMapEntry<>("key-2", "value-A"),
                new SimpleMapEntry<>("key-C", "value-3")
        ));

        assertThat(result, is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldContainEntries(String sSerializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String> cache = createClient(sSerializerName, serializer);

        Set<Map.Entry<String, String>> entries = cache.entrySet();

        for (Map.Entry<String, String> entry : s_realCache.entrySet())
            {
            boolean result = entries.contains(entry);
            assertThat("EntrySet should contain entry " + entry, result, is(true));
            }
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldNotContainEntry(String sSerializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String> cache = createClient(sSerializerName, serializer);

        Set<Map.Entry<String, String>> entries = cache.entrySet();
        boolean                        result  = entries.contains(new SimpleMapEntry<>("key-A", "value-A"));
        assertThat(result, is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldNotContainEntryWithValueMismatch(String sSerializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String> cache = createClient(sSerializerName, serializer);

        Set<Map.Entry<String, String>> entries = cache.entrySet();
        boolean                        result  = entries.contains(new SimpleMapEntry<>("key-1", "value-2"));
        assertThat(result, is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldBeEqual(String sSerializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String>     cache = createClient(sSerializerName, serializer);
        Set<Map.Entry<String, String>> set   = new HashSet<>(s_realCache.entrySet());

        Set<Map.Entry<String, String>> entries = cache.entrySet();
        assertThat(entries.equals(set), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldNotBeEqual(String sSerializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String>     cache = createClient(sSerializerName, serializer);
        Set<Map.Entry<String, String>> set   = new HashSet<>(Arrays.asList(
                new SimpleMapEntry<>("key-1", "value-1"),
                new SimpleMapEntry<>("key-2", "value-A"),
                new SimpleMapEntry<>("key-C", "value-3")
        ));

        Set<Map.Entry<String, String>> entries = cache.entrySet();
        assertThat(entries.equals(set), is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldHaveSameHashCode(String sSerializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String>     cache = createClient(sSerializerName, serializer);
        Set<Map.Entry<String, String>> set   = new HashSet<>(s_realCache.entrySet());

        Set<Map.Entry<String, String>> entries = cache.entrySet();
        assertThat(entries.hashCode(), is(set.hashCode()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldRemoveAll(String sSerializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String>     cache = createClient(sSerializerName, serializer);
        Set<Map.Entry<String, String>> set   = new HashSet<>(Arrays.asList(
                new SimpleMapEntry<>("key-1", "value-1"),
                new SimpleMapEntry<>("key-2", "value-2"),
                new SimpleMapEntry<>("key-3", "value-3"),
                new SimpleMapEntry<>("key-4", "value-A"),
                new SimpleMapEntry<>("key-C", "value-6")
        ));

        Set<Map.Entry<String, String>> entries = cache.entrySet();
        boolean                        result  = entries.removeAll(set);
        assertThat(result, is(true));
        assertThat(s_realCache.size(), is(7));
        assertThat(s_realCache.containsKey("key-1"), is(false));
        assertThat(s_realCache.containsKey("key-2"), is(false));
        assertThat(s_realCache.containsKey("key-3"), is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldNotRemoveAll(String sSerializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String>     cache = createClient(sSerializerName, serializer);
        Set<Map.Entry<String, String>> set   = new HashSet<>(Arrays.asList(
                new SimpleMapEntry<>("key-A", "value-1"),
                new SimpleMapEntry<>("key-B", "value-2"),
                new SimpleMapEntry<>("key-C", "value-3")
        ));

        Set<Map.Entry<String, String>> entries = cache.entrySet();
        boolean                        result  = entries.removeAll(set);
        assertThat(result, is(false));
        assertThat(s_realCache.size(), is(10));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldRetainAll(String sSerializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String>     cache = createClient(sSerializerName, serializer);
        Set<Map.Entry<String, String>> set   = new HashSet<>(Arrays.asList(
                new SimpleMapEntry<>("key-1", "value-1"),
                new SimpleMapEntry<>("key-2", "value-2"),
                new SimpleMapEntry<>("key-3", "value-3"),
                new SimpleMapEntry<>("key-4", "value-A"),
                new SimpleMapEntry<>("key-C", "value-6")
        ));

        Set<Map.Entry<String, String>> entries = cache.entrySet();
        boolean                        result  = entries.retainAll(set);
        assertThat(result, is(true));
        assertThat(s_realCache.size(), is(3));
        assertThat(s_realCache.containsKey("key-1"), is(true));
        assertThat(s_realCache.containsKey("key-2"), is(true));
        assertThat(s_realCache.containsKey("key-3"), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldRetainAllWithNoneMatchingAndAllRemoved(String sSerializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String>     cache = createClient(sSerializerName, serializer);
        Set<Map.Entry<String, String>> set   = new HashSet<>(Arrays.asList(
                new SimpleMapEntry<>("key-4", "value-A"),
                new SimpleMapEntry<>("key-C", "value-6")
        ));

        Set<Map.Entry<String, String>> entries = cache.entrySet();
        boolean                        result  = entries.retainAll(set);
        assertThat(result, is(true));
        assertThat(s_realCache.size(), is(0));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldRetainAllWhereAllMatchAndNoneRemoved(String sSerializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String> cache = createClient(sSerializerName, serializer);

        Set<Map.Entry<String, String>> entries = cache.entrySet();
        boolean                        result  = entries.retainAll(s_realCache.entrySet());
        assertThat(result, is(false));
        assertThat(s_realCache.size(), is(10));
        }

    // ToDo: enable this when entrySet(Filter) is implemented
    //@ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldConvertToObjectArray(String sSerializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String> cache = createClient(sSerializerName, serializer);

        Set<Map.Entry<String, String>> entries  = cache.entrySet();
        Object[]                       result   = entries.toArray();
        Object[]                       expected = s_realCache.entrySet().toArray();
        assertThat(result, is(expected));
        }

    // ToDo: enable this when entrySet(Filter) is implemented
    //@ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldConvertToArray(String sSerializerName, Serializer serializer)
        {
        clearAndPopulateRealCache(10);
        NamedCache<String, String> cache = createClient(sSerializerName, serializer);

        Set<Map.Entry<String, String>> entries  = cache.entrySet();
        Map.Entry[]                    result   = entries.toArray(new Map.Entry[0]);
        Map.Entry[]                    expected = s_realCache.entrySet().toArray(new Map.Entry[0]);
        assertThat(result, is(expected));
        }

    // ----- helper methods -------------------------------------------------

    protected void clearAndPopulateRealCache(int count)
        {
        s_realCache.clear();
        for (int i = 0; i < count; i++)
            {
            s_realCache.put("key-" + i, "value-" + i);
            }
        }

    /**
     * Obtain the {@link com.tangosol.io.Serializer} instances to use for parameterized
     * test {@link org.junit.jupiter.params.provider.Arguments}.
     *
     * @return the {@link com.tangosol.io.Serializer} instances to use for test
     * {@link org.junit.jupiter.params.provider.Arguments}
     */
    protected static Stream<Arguments> serializers()
        {
        List<Arguments> args   = new ArrayList<>();
        ClassLoader     loader = Base.getContextClassLoader();

        args.add(Arguments.of("", new DefaultSerializer()));

        OperationalContext ctx = (OperationalContext) CacheFactory.getCluster();
        for (Map.Entry<String, SerializerFactory> entry : ctx.getSerializerMap().entrySet())
            {
            args.add(Arguments.of(entry.getKey(), entry.getValue().createSerializer(loader)));
            }

        return args.stream();
        }

    // ----- constants ------------------------------------------------------

    protected static final String CACHE_NAME = "testCache";

    // ----- data members ---------------------------------------------------

    protected static NamedCache<String, String> s_realCache;

    @RegisterExtension
    protected static ServerHelper s_serverHelper = new ServerHelper()
            .setProperty("coherence.ttl", "0")
            .setProperty("coherence.wka", "127.0.0.1")
            .setProperty("coherence.localhost", "127.0.0.1")
            .setProperty("coherence.cluster", "NamedCacheServiceEntrySetIT")
            .setProperty("coherence.override", "coherence-json-override.xml")
            .setProperty("coherence.pof.config", "test-pof-config.xml")
            .setProperty("coherence.cacheconfig", "coherence-config.xml");
    }
