package io.machinecode.vial.bench.perf.set.add;

import com.carrotsearch.hppc.ObjectSet;
import com.gs.collections.impl.set.mutable.UnifiedSet;
import gnu.trove.set.hash.THashSet;
import io.machinecode.vial.core.set.OHashSet;
import net.openhft.koloboke.collect.hash.HashConfig;
import net.openhft.koloboke.collect.set.hash.HashObjSets;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Timeout;
import org.openjdk.jmh.annotations.Warmup;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.SingleShotTime})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 10, batchSize = 1000000)
@Measurement(iterations = 20, batchSize = 1000000)
@Timeout(time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class OHashSetAdd {

    @Param({"0.75"})
    float factor;

    @Param({"8", "1000000"})
    int capacity;

    private Random r;

    private Set<Long> vial;
    private Set<Long> jdk;
    private Set<Long> trove;
    private Set<Long> fastutil;
    private ObjectSet<Long> hppc;
    private Set<Long> koloboke;
    private Set<Long> gs;

    @Setup(Level.Iteration)
    public void init() {
        r = new Random();
        r.setSeed(0x654265);
        vial = new OHashSet<>(capacity, factor);
        jdk = new HashSet<>(capacity, factor);
        trove = new THashSet<>(capacity, factor);
        fastutil = new it.unimi.dsi.fastutil.objects.ObjectOpenHashSet<>(capacity);
        hppc = new com.carrotsearch.hppc.ObjectOpenHashSet<>(capacity, factor);
        koloboke = HashObjSets.getDefaultFactory()
                .withHashConfig(HashConfig.fromLoads(Math.max(factor / 2, 0.1), factor, Math.min(factor * 2, 0.9)))
                .newMutableSet(capacity);
        gs = new UnifiedSet<>(capacity, factor);
    }

    @Benchmark
    public boolean vial() {
        final Long key = r.nextLong();
        return vial.add(key);
    }

    @Benchmark
    public boolean jdk() {
        final Long key = r.nextLong();
        return jdk.add(key);
    }

    @Benchmark
    public boolean trove() {
        final Long key = r.nextLong();
        return trove.add(key);
    }

    @Benchmark
    public boolean fastutil() {
        final Long key = r.nextLong();
        return fastutil.add(key);
    }

    @Benchmark
    public boolean hppc() {
        final Long key = r.nextLong();
        return hppc.add(key);
    }

    @Benchmark
    public boolean koloboke() {
        final Long key = r.nextLong();
        return koloboke.add(key);
    }

    @Benchmark
    public boolean gs() {
        final Long key = r.nextLong();
        return gs.add(key);
    }
}
