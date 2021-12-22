package com.seibel.lod.common.wrappers.worldGeneration;

import com.seibel.lod.core.builders.lodBuilding.LodBuilder;
import com.seibel.lod.core.builders.lodBuilding.LodBuilderConfig;
import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.objects.lod.LodDimension;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.seibel.lod.common.wrappers.chunk.ChunkWrapper;
import com.seibel.lod.common.wrappers.worldGeneration.WorldGenerationStep.Steps;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.entity.ai.village.VillageSiege;
import net.minecraft.world.entity.npc.CatSpawner;
import net.minecraft.world.entity.npc.WanderingTraderSpawner;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.PatrolSpawner;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;

public class WorldGenerationStep {

	/*
	public static class ChunkScanner implements ChunkScanAccess {

		@Override
		public CompletableFuture<Void> scanChunk(ChunkPos paramChunkPos, StreamTagVisitor paramStreamTagVisitor) {
			// TODO Auto-generated method stub
			return null;
		}

	}*/


    public static class GenerationEvent {
        private static int generationFutureDebugIDs = 0;
        ChunkPos pos;
        int range;
        Future<?> future;
        long nanotime;
        int id;
        Steps target;

        public GenerationEvent(ChunkPos pos, int range, WorldGenerationStep generationGroup, Steps target) {
            nanotime = System.nanoTime();
            this.pos = pos;
            this.range = range;
            id = generationFutureDebugIDs++;
            this.target = target;
            future = generationGroup.executors.submit(() -> {
                generationGroup.generateLodFromList(this);
            });
        }
        public boolean isCompleted() {
            return future.isDone();
        }
        public boolean hasTimeout(int duration, TimeUnit unit) {
            long currentTime = System.nanoTime();
            long delta = currentTime - nanotime;
            return (delta > TimeUnit.NANOSECONDS.convert(duration, unit));
        }
        public void terminate() {
            future.cancel(true);
        }
        public void join() {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        public boolean tooClose(int cx, int cz, int cr) {
            int dist = Math.min(Math.abs(cx - pos.x), Math.abs(cz - pos.z));
            return dist<range+cr;
        }
        public void refreshTimeout() {
            nanotime = System.nanoTime();
        }

        @Override
        public String toString() {
            return id + ":"+ range + "@"+ pos+"("+target+")";
        }
    }






    private static <T> T joinAsync(CompletableFuture<T> f) {
        //while (!f.isDone()) Thread.yield();
        return f.join();
    }

    ServerLevel level;
    ChunkGenerator generator;
    StructureManager structures;
    BiomeManager biomeManager;
    WorldGenSettings worldGenSettings;
    ThreadedLevelLightEngine lightEngine;
    LodBuilder lodBuilder;
    LodDimension lodDim;
    StructureFeatureManager structureFeatureManager;
//    StructureCheck structureCheck;
    Registry<Biome> biomes;
    RegistryAccess registry;
    long worldSeed;
    //public ExecutorService executors = Executors.newWorkStealingPool();
    public ExecutorService executors = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("Gen-Worker-Thread-%d").build());

    //public ExecutorService executors = Executors.newFixedThreadPool(8, new ThreadFactoryBuilder().setNameFormat("Gen-Worker-Thread-%d").build());

    public WorldGenerationStep(ServerLevel level, LodBuilder lodBuilder, LodDimension lodDim) {
        System.out.println("================WORLD_GEN_STEP_INITING=============");
        this.level = level;
        this.lodBuilder = lodBuilder;
        this.lodDim = lodDim;
        setupStuff();

        StepStructureStart.onLevelLoad(generator, worldGenSettings, registry, structureFeatureManager, structures, worldSeed);
        StepStructureReference.onLevelLoad(level, generator, structureFeatureManager);
        StepBiomes.onLevelLoad(level, generator, biomes, structureFeatureManager);
        StepNoise.onLevelLoad(level, generator, structureFeatureManager);
        StepSurface.onLevelLoad(level, generator, structureFeatureManager);
        StepCarvers.onLevelLoad(level, generator, structureFeatureManager, worldSeed, biomeManager);
        StepFeatures.onLevelLoad(level, generator, structureFeatureManager, lightEngine);
        StepLight.onLevelLoad(lightEngine);

    }


    private void setupStuff() {
        lightEngine = (ThreadedLevelLightEngine) level.getLightEngine();
        MinecraftServer server = level.getServer();
        WorldData worldData = server.getWorldData();
        worldGenSettings = worldData.worldGenSettings();
        registry = server.registryAccess();
        biomes = registry.registryOrThrow(Registry.BIOME_REGISTRY);
        worldSeed = worldGenSettings.seed();
        long biomeSeed = BiomeManager.obfuscateSeed(worldSeed);
        // FIXME: broken in 1.17.1
//        biomeManager = new BiomeManager(level, biomeSeed);
        structures = server.getStructureManager();
        // TODO: Get the current level dimension
        MappedRegistry<LevelStem> mappedRegistry = worldGenSettings.dimensions();
        LevelStem levelStem = mappedRegistry.get(LevelStem.OVERWORLD);
        if (levelStem == null) {
            throw new RuntimeException("There should already be a level.... Right???");
        } else {
            generator = levelStem.generator();
        }
        structureFeatureManager = new StructureFeatureManager(level, worldGenSettings);
    }

    public static final class GridList<T> extends ArrayList<T> implements List<T> {

        private static final long serialVersionUID = 1585978374811888116L;
        public final int gridCentreToEdge;
        public final int gridSize;

        public GridList(int gridCentreToEdge) {
            super((gridCentreToEdge * 2 + 1) * (gridCentreToEdge * 2 + 1));
            gridSize = gridCentreToEdge * 2 + 1;
            this.gridCentreToEdge = gridCentreToEdge;
        }

        public final int offsetOf(int index, int x, int y) {
            return index + x + y * gridSize;
        }

        public GridList<T> subGrid(int centreIndex, int gridCentreToEdge) {
            GridList<T> subGrid = new GridList<T>(gridCentreToEdge);
            for (int oy = -gridCentreToEdge; oy <= gridCentreToEdge; oy++) {
                int begin = offsetOf(centreIndex, -gridCentreToEdge, oy);
                int end = offsetOf(centreIndex, gridCentreToEdge, oy);
                subGrid.addAll(this.subList(begin, end+1));
            }

            //System.out.println("========================================\n"+
            //this.toDetailString() + "\nTOOOOOOOOOOOOO\n"+subGrid.toDetailString()+
            //"==========================================\n");
            return subGrid;
        }

        @Override
        public String toString() {
            return "GridList "+gridSize+"*"+gridSize+"["+size()+"]";
        }
        public String toDetailString() {
            StringBuilder str = new StringBuilder("\n");
            int i = 0;
            for (T t : this) {
                str.append(t.toString());
                str.append(", ");
                i++;
                if (i%gridSize == 0) {
                    str.append("\n");
                }
            }
            return str.toString();
        }
    }

    public static class ChunkSynconizer {

        private ReentrantLock uniqueOwnerLock = new ReentrantLock();
        ChunkAccess chunk;
        Steps completedStep = Steps.Empty;

        public ChunkSynconizer(ChunkPos pos, ServerLevel level) {
            chunk = new ProtoChunk(pos, UpgradeData.EMPTY, level);
        }

        public boolean tryClaimOwnerLock() {
            return uniqueOwnerLock.tryLock();
        }

        public void releaseOwnerLock() {
            uniqueOwnerLock.unlock();
        }

        public boolean hasCompletedStep(Steps step) {
            return step.compareTo(completedStep) <= 0;
        }

        public void set(ChunkAccess newChunk, Steps newStep) {
            chunk = newChunk;
            completedStep = newStep;
        }

        public void set(Steps newStep) {
            completedStep = newStep;
        }

        @Override
        public String toString() {
            return chunk.getPos().toString();
        }
    }

    ConcurrentHashMap<Long, ChunkSynconizer> chunks = new ConcurrentHashMap<Long, ChunkSynconizer>();
    // No longer using Long2ObjectLinkedOpenHashMap as I doubt it is multithread
    // safe.

    private static final long toLongPos(int cx, int cy) {
        return ChunkPos.asLong(cx, cy);
    }

    private final ChunkSynconizer getChunkSynconizer(long pos) {
        ChunkSynconizer chunk = chunks.get(pos);
        if (chunk != null)
            return chunk;
        chunk = new ChunkSynconizer(new ChunkPos(pos), level);
        ChunkSynconizer oldVal = chunks.putIfAbsent(pos, chunk);
        if (oldVal != null)
            return oldVal;
        return chunk;
    }

    public void generateLodFromList(GenerationEvent event) {
        try {
            System.out.println("Started event: "+event);
            GridList<ChunkSynconizer> referencedChunks;
            DistanceGenerationMode generationMode;
            Runnable lambda = () -> {event.refreshTimeout();};
            switch (event.target) {
                case Empty:
                    return;
                case StructureStart:
                    referencedChunks = generateStructureStart(lambda, event.pos, event.range);
                    generationMode = DistanceGenerationMode.NONE;
                    break;
                case StructureReference:
                    referencedChunks = generateStructureReference(lambda, event.pos, event.range);
                    generationMode = DistanceGenerationMode.NONE;
                    break;
                case Biomes:
                    referencedChunks = generateBiomes(lambda, event.pos, event.range);
                    generationMode = DistanceGenerationMode.BIOME_ONLY_SIMULATE_HEIGHT;
                    break;
                case Noise:
                    referencedChunks = generateNoise(lambda, event.pos, event.range);
                    generationMode = DistanceGenerationMode.BIOME_ONLY_SIMULATE_HEIGHT;
                    break;
                case Surface:
                    referencedChunks = generateSurface(lambda, event.pos, event.range);
                    generationMode = DistanceGenerationMode.SURFACE;
                    break;
                case Carvers:
                    referencedChunks = generateCarvers(lambda, event.pos, event.range);
                    generationMode = DistanceGenerationMode.SURFACE;
                    break;
                case Features:
                    referencedChunks = generateFeatures(lambda, event.pos, event.range);
                    generationMode = DistanceGenerationMode.FEATURES;
                    break;
                case LiquidCarvers:
                    return;
                case Light:
                    return;
                default:
                    return;
            }
            int centreIndex = referencedChunks.size() / 2;

            for (int ox = -event.range; ox <= event.range; ox++) {
                for (int oy = -event.range; oy <= event.range; oy++) {
                    int targetIndex = referencedChunks.offsetOf(centreIndex, ox, oy);
                    ChunkSynconizer target = referencedChunks.get(targetIndex);
                    lodBuilder.generateLodNodeFromChunk(lodDim, new ChunkWrapper(target.chunk), new LodBuilderConfig(generationMode));
                }
            }
            lambda.run();
            for (ChunkSynconizer sync : referencedChunks) {
                chunks.remove(sync.chunk.getPos().toLong());
            }
            System.out.println("Ended event: "+event);
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public GridList<ChunkSynconizer> generateStructureStart(Runnable r, ChunkPos pos, int range) {
        int cx = pos.x;
        int cy = pos.z;
        GridList<ChunkSynconizer> chunks = new GridList<ChunkSynconizer>(range);

        for (int oy = -range; oy <= range; oy++) {
            for (int ox = -range; ox <= range; ox++) {
                ChunkSynconizer target = getChunkSynconizer(toLongPos(cx + ox, cy + oy));
                chunks.add(target);
                if (!target.hasCompletedStep(Steps.StructureStart)) {

                    boolean owned = target.tryClaimOwnerLock();
                    if (owned) {
                        try {
                            ChunkAccess access = target.chunk;
                            target.set(StepStructureStart.generate(access),
                                    Steps.StructureStart);
                        } finally {
                            target.releaseOwnerLock();
                        }
                    }



                }
            }
        }
        r.run();
        return chunks;
    }

    public GridList<ChunkSynconizer> generateStructureReference(Runnable r, ChunkPos pos, int range) {
        int prestepRange = range + StepStructureReference.RANGE;
        GridList<ChunkSynconizer> referencedChunks = generateStructureStart(r, pos, prestepRange);
        int centreIndex = referencedChunks.size() / 2;

        for (int oy = -range; oy <= range; oy++) {
            for (int ox = -range; ox <= range; ox++) {
                int targetIndex = referencedChunks.offsetOf(centreIndex, ox, oy);
                ChunkSynconizer target = referencedChunks.get(targetIndex);
                if (!target.hasCompletedStep(Steps.StructureReference)) {
                    boolean owned = target.tryClaimOwnerLock();
                    if (owned) {
                        try {
                            GridList<ChunkSynconizer> reference = referencedChunks.subGrid(targetIndex,
                                    StepStructureReference.RANGE);
                            ArrayList<ChunkAccess> referenceAccess = new ArrayList<ChunkAccess>(reference.size());
                            for (ChunkSynconizer ref : reference) {
                                referenceAccess.add(ref.chunk);
                            }
                            StepStructureReference.generate(referenceAccess, target.chunk);
                            target.set(Steps.StructureReference);
                        } finally {
                            target.releaseOwnerLock();
                        }
                    }
                }
            }
        }
        r.run();
        return referencedChunks;
    }

    public GridList<ChunkSynconizer> generateBiomes(Runnable r, ChunkPos pos, int range) {
        int prestepRange = range + 1;
        GridList<ChunkSynconizer> referencedChunks = generateStructureReference(r, pos, prestepRange);
        int centreIndex = referencedChunks.size() / 2;

        for (int oy = -range; oy <= range; oy++) {
            for (int ox = -range; ox <= range; ox++) {
                int targetIndex = referencedChunks.offsetOf(centreIndex, ox, oy);
                ChunkSynconizer target = referencedChunks.get(targetIndex);
                if (!target.hasCompletedStep(Steps.Biomes)) {
                    boolean owned = target.tryClaimOwnerLock();
                    if (owned) {
                        try {
                            GridList<ChunkSynconizer> reference = referencedChunks.subGrid(targetIndex,
                                    StepBiomes.RANGE);
                            ArrayList<ChunkAccess> referenceAccess = new ArrayList<ChunkAccess>(reference.size());
                            for (ChunkSynconizer ref : reference) {
                                referenceAccess.add(ref.chunk);
                            }
                            target.set(StepBiomes.generate(referenceAccess, target.chunk, executors), Steps.Biomes);
                        } finally {
                            target.releaseOwnerLock();
                        }
                    }
                }
            }
        }
        r.run();
        return referencedChunks;
    }

    public GridList<ChunkSynconizer> generateNoise(Runnable r, ChunkPos pos, int range) {
        int prestepRange = range + 1;
        GridList<ChunkSynconizer> referencedChunks = generateBiomes(r, pos, prestepRange);
        int centreIndex = referencedChunks.size() / 2;

        for (int oy = -range; oy <= range; oy++) {
            for (int ox = -range; ox <= range; ox++) {
                int targetIndex = referencedChunks.offsetOf(centreIndex, ox, oy);
                ChunkSynconizer target = referencedChunks.get(targetIndex);
                if (!target.hasCompletedStep(Steps.Noise)) {
                    boolean owned = target.tryClaimOwnerLock();
                    if (owned) {
                        try {
                            GridList<ChunkSynconizer> reference = referencedChunks.subGrid(targetIndex,
                                    StepNoise.RANGE);
                            ArrayList<ChunkAccess> referenceAccess = new ArrayList<ChunkAccess>(reference.size());
                            for (ChunkSynconizer ref : reference) {
                                referenceAccess.add(ref.chunk);
                            }
                            target.set(StepNoise.generate(referenceAccess, target.chunk, executors),
                                    Steps.Noise);
                        } finally {
                            target.releaseOwnerLock();
                        }
                    }
                }
            }
        }
        r.run();
        return referencedChunks;
    }

    public GridList<ChunkSynconizer> generateSurface(Runnable r, ChunkPos pos, int range) {
        int prestepRange = range + 1;
        GridList<ChunkSynconizer> referencedChunks = generateNoise(r, pos, prestepRange);
        int centreIndex = referencedChunks.size() / 2;

        for (int oy = -range; oy <= range; oy++) {
            for (int ox = -range; ox <= range; ox++) {
                int targetIndex = referencedChunks.offsetOf(centreIndex, ox, oy);
                ChunkSynconizer target = referencedChunks.get(targetIndex);
                if (!target.hasCompletedStep(Steps.Surface)) {
                    boolean owned = target.tryClaimOwnerLock();
                    if (owned) {
                        try {
                            GridList<ChunkSynconizer> reference = referencedChunks.subGrid(targetIndex,
                                    StepSurface.RANGE);
                            ArrayList<ChunkAccess> referenceAccess = new ArrayList<ChunkAccess>(reference.size());
                            for (ChunkSynconizer ref : reference) {
                                referenceAccess.add(ref.chunk);
                            }

                            target.set(StepSurface.generate(referenceAccess, target.chunk),
                                    Steps.Surface);
                        } finally {
                            target.releaseOwnerLock();
                        }
                    }
                }
            }
        }
        r.run();
        return referencedChunks;
    }


    public GridList<ChunkSynconizer> generateCarvers(Runnable r, ChunkPos pos, int range) {
        int prestepRange = range + 1;
        GridList<ChunkSynconizer> referencedChunks = generateSurface(r, pos, prestepRange);
        int centreIndex = referencedChunks.size() / 2;

        for (int oy = -range; oy <= range; oy++) {
            for (int ox = -range; ox <= range; ox++) {
                int targetIndex = referencedChunks.offsetOf(centreIndex, ox, oy);
                ChunkSynconizer target = referencedChunks.get(targetIndex);
                if (!target.hasCompletedStep(Steps.Carvers)) {
                    boolean owned = target.tryClaimOwnerLock();
                    if (owned) {
                        try {
                            GridList<ChunkSynconizer> reference = referencedChunks.subGrid(targetIndex,
                                    StepCarvers.RANGE);
                            ArrayList<ChunkAccess> referenceAccess = new ArrayList<ChunkAccess>(reference.size());
                            for (ChunkSynconizer ref : reference) {
                                referenceAccess.add(ref.chunk);
                            }
                            target.set(StepCarvers.generate(referenceAccess, target.chunk),
                                    Steps.Carvers);
                        } finally {
                            target.releaseOwnerLock();
                        }
                    }
                }
            }
        }
        r.run();
        return referencedChunks;
    }


    public GridList<ChunkSynconizer> generateFeatures(Runnable r, ChunkPos pos, int range) {
        int prestepRange = range + 1;
        GridList<ChunkSynconizer> referencedChunks = generateCarvers(r, pos, prestepRange);
        int centreIndex = referencedChunks.size() / 2;

        for (int oy = -range; oy <= range; oy++) {
            for (int ox = -range; ox <= range; ox++) {
                int targetIndex = referencedChunks.offsetOf(centreIndex, ox, oy);
                ChunkSynconizer target = referencedChunks.get(targetIndex);
                if (!target.hasCompletedStep(Steps.Features)) {
                    boolean owned = target.tryClaimOwnerLock();
                    if (owned) {
                        try {
                            GridList<ChunkSynconizer> reference = referencedChunks.subGrid(targetIndex,
                                    StepFeatures.RANGE);
                            ArrayList<ChunkAccess> referenceAccess = new ArrayList<ChunkAccess>(reference.size());
                            for (ChunkSynconizer ref : reference) {
                                referenceAccess.add(ref.chunk);
                            }
                            target.set(StepFeatures.generate(referenceAccess, target.chunk),
                                    Steps.Features);
                        } finally {
                            target.releaseOwnerLock();
                        }
                    }
                }
            }
        }
        r.run();
        return referencedChunks;
    }




    enum Steps {
        Empty, StructureStart, StructureReference, Biomes, Noise, Surface, Carvers, LiquidCarvers, Features, Light,
    }

    public static class StepStructureStart {
        public static final ChunkStatus STATUS = ChunkStatus.STRUCTURE_STARTS;
        public static final int RANGE = STATUS.getRange();
        public static final EnumSet<Heightmap.Types> HEIGHTMAP_TYPES = STATUS.heightmapsAfter();
        private static ChunkGenerator gen;
        private static boolean doGenerateFeatures = true;
        private static RegistryAccess registry;
        private static StructureFeatureManager structFeat;
        private static StructureManager struct;
        private static long seed;

        public final static void onLevelLoad(ChunkGenerator generator, WorldGenSettings genSettings, RegistryAccess registryAccess,
                                                StructureFeatureManager structureFeature, StructureManager structures, long worldSeed) {
            gen = generator;
            doGenerateFeatures = genSettings.generateFeatures();
            registry = registryAccess;
            structFeat = structureFeature;
            struct = structures;
            seed = worldSeed;
        }

        public final static ChunkAccess generate(ChunkAccess chunk) {
            if (doGenerateFeatures) {
                // Should be thread safe
                gen.createStructures(registry, structFeat, chunk, struct, seed);
            }
            ((ProtoChunk) chunk).setStatus(STATUS);
            return chunk;
        }

        static ChunkAccess load(ServerLevel level, ChunkAccess chunk) {
            ((ProtoChunk) chunk).setStatus(STATUS);
            return chunk;
        }
    }

    public static class StepStructureReference {
        public static final ChunkStatus STATUS = ChunkStatus.STRUCTURE_REFERENCES;
        public static final int RANGE = STATUS.getRange();
        public static final EnumSet<Heightmap.Types> HEIGHTMAP_TYPES = STATUS.heightmapsAfter();
        private static ChunkGenerator gen;
        private static StructureFeatureManager structFeat;
        private static ServerLevel level;

        public final static void onLevelLoad(ServerLevel serverLevel, ChunkGenerator generator, StructureFeatureManager structureFeature) {
            gen = generator;
            structFeat = structureFeature;
            level = serverLevel;
        }

        public static final void generate(List<ChunkAccess> chunkList, ChunkAccess chunk) {
            WorldGenRegion worldGenRegion = new WorldGenRegion(level, chunkList, STATUS, -1);
            // Note: Not certain StructureFeatureManager.forWorldGenRegion(...) is thread safe
            gen.createReferences(worldGenRegion, structFeat.forWorldGenRegion(worldGenRegion), chunk);
            ((ProtoChunk) chunk).setStatus(STATUS);
        }

        static ChunkAccess load(ChunkAccess chunk) {
            ((ProtoChunk) chunk).setStatus(STATUS);
            return chunk;
        }
    }

    public static class StepBiomes {
        public static final ChunkStatus STATUS = ChunkStatus.BIOMES;
        public static final int RANGE = STATUS.getRange();
        public static final EnumSet<Heightmap.Types> HEIGHTMAP_TYPES = STATUS.heightmapsAfter();
        public static Registry<Biome> biomeRegistry;
        private static ChunkGenerator gen;
        private static StructureFeatureManager structFeat;
        private static ServerLevel level;

        public final static void onLevelLoad(ServerLevel serverLevel, ChunkGenerator generator, Registry<Biome> registry, StructureFeatureManager structureFeature) {
            biomeRegistry = registry;
            gen = generator;
            structFeat = structureFeature;
            level = serverLevel;
        }

        public static final ChunkAccess generate(List<ChunkAccess> chunkList, ChunkAccess chunk, Executor worker) {
            WorldGenRegion worldGenRegion = new WorldGenRegion(level, chunkList, STATUS, -1);
            chunk = joinAsync(gen.fillFromNoise(worker, structFeat.forWorldGenRegion(worldGenRegion), chunk));
            ((ProtoChunk) chunk).setStatus(STATUS);
            return chunk;
        }

        static ChunkAccess load(ChunkAccess chunk) {
            ((ProtoChunk) chunk).setStatus(STATUS);
            return chunk;
        }
    }

    public static class StepNoise {
        public static final ChunkStatus STATUS = ChunkStatus.NOISE;
        public static final int RANGE = STATUS.getRange();
        public static final EnumSet<Heightmap.Types> HEIGHTMAP_TYPES = STATUS.heightmapsAfter();
        private static ChunkGenerator gen;
        private static StructureFeatureManager structFeat;
        private static ServerLevel level;

        public final static void onLevelLoad(ServerLevel serverLevel, ChunkGenerator generator, StructureFeatureManager structureFeature) {
            gen = generator;
            structFeat = structureFeature;
            level = serverLevel;
        }

        public static final ChunkAccess generate(List<ChunkAccess> chunkList, ChunkAccess chunk, Executor worker) {
            WorldGenRegion worldGenRegion = new WorldGenRegion(level, chunkList, STATUS, 0);
            chunk = joinAsync(gen.fillFromNoise(worker, structFeat.forWorldGenRegion(worldGenRegion), chunk));
            ((ProtoChunk) chunk).setStatus(STATUS);
            return chunk;
        }

        static ChunkAccess load(ChunkAccess chunk) {
            ((ProtoChunk) chunk).setStatus(STATUS);
            return chunk;
        }
    }

    public static class StepSurface {
        public static final ChunkStatus STATUS = ChunkStatus.SURFACE;
        public static final int RANGE = STATUS.getRange();
        public static final EnumSet<Heightmap.Types> HEIGHTMAP_TYPES = STATUS.heightmapsAfter();
        private static ChunkGenerator gen;
        private static StructureFeatureManager structFeat;
        private static ServerLevel level;

        public final static void onLevelLoad(ServerLevel serverLevel, ChunkGenerator generator, StructureFeatureManager structureFeature) {
            gen = generator;
            structFeat = structureFeature;
            level = serverLevel;
        }

        public static final ChunkAccess generate(List<ChunkAccess> chunkList, ChunkAccess chunk) {
            WorldGenRegion worldGenRegion = new WorldGenRegion(level, chunkList, STATUS, 0);
            gen.buildSurfaceAndBedrock(worldGenRegion, chunk);
            ((ProtoChunk) chunk).setStatus(STATUS);
            return chunk;
        }

        static ChunkAccess load(ChunkAccess chunk) {
            ((ProtoChunk) chunk).setStatus(STATUS);
            return chunk;
        }
    }

    public static class StepCarvers {
        public static final ChunkStatus STATUS = ChunkStatus.CARVERS;
        public static final int RANGE = STATUS.getRange();
        public static final EnumSet<Heightmap.Types> HEIGHTMAP_TYPES = STATUS.heightmapsAfter();
        private static ChunkGenerator gen;
        private static StructureFeatureManager structFeat;
        private static ServerLevel level;
        private static long seed;
        private static BiomeManager biomes;

        public final static void onLevelLoad(ServerLevel serverLevel, ChunkGenerator generator, StructureFeatureManager structureFeature,
                                             long worldSeed, BiomeManager biomeManger) {
            gen = generator;
            structFeat = structureFeature;
            level = serverLevel;
            seed = worldSeed;
            biomes = biomeManger;
        }


        public static final ChunkAccess generate(List<ChunkAccess> chunkList, ChunkAccess chunk) {
            gen.applyCarvers(seed, biomes, chunk, GenerationStep.Carving.AIR);
            ((ProtoChunk) chunk).setStatus(STATUS);
            return chunk;
        }

        static ChunkAccess load(ChunkAccess chunk) {
            ((ProtoChunk) chunk).setStatus(STATUS);
            return chunk;
        }
    }

    public static class StepLiquidCarvers {
        public static final ChunkStatus STATUS = ChunkStatus.LIQUID_CARVERS;
        public static final int RANGE = STATUS.getRange();
        public static final EnumSet<Heightmap.Types> HEIGHTMAP_TYPES = STATUS.heightmapsAfter();

        public static final ChunkAccess generate(List<ChunkAccess> chunkList, ChunkAccess chunk, Executor worker) {
            // FIXME: I think the decompiler failed on this one. Find the actual body and
            // put it here.
            ((ProtoChunk) chunk).setStatus(STATUS);
            return chunk;
        }

        static ChunkAccess load(ChunkAccess chunk) {
            ((ProtoChunk) chunk).setStatus(STATUS);
            return chunk;
        }
    }

    public static class StepFeatures {
        public static final ChunkStatus STATUS = ChunkStatus.FEATURES;
        public static final int RANGE = STATUS.getRange();
        public static final EnumSet<Heightmap.Types> HEIGHTMAP_TYPES = STATUS.heightmapsAfter();
        private static ChunkGenerator gen;
        private static StructureFeatureManager structFeat;
        private static ServerLevel level;
        private static LevelLightEngine lights;

        public final static void onLevelLoad(ServerLevel serverLevel, ChunkGenerator generator, StructureFeatureManager structureFeature,
                                             LevelLightEngine lightEngine) {
            gen = generator;
            structFeat = structureFeature;
            level = serverLevel;
            lights = lightEngine;
        }

        private static ReentrantLock testLock = new ReentrantLock();
        public static final ChunkAccess generate(List<ChunkAccess> chunkList, ChunkAccess chunk) {
            ProtoChunk protoChunk = (ProtoChunk) chunk;
            if (chunk.getStatus() == STATUS) return chunk;
            testLock.lock();
            try {
                if (chunk.getStatus() != STATUS) {

                    protoChunk.setLightEngine(lights);

                    Heightmap.primeHeightmaps(chunk,
                            EnumSet.of(Heightmap.Types.MOTION_BLOCKING, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                    Heightmap.Types.OCEAN_FLOOR, Heightmap.Types.WORLD_SURFACE));

                    // This could be problematic. May need to lock the 8 surrounding chunks then.
                    WorldGenRegion worldGenRegion = new WorldGenRegion(level, chunkList, STATUS, 1);

                    gen.applyBiomeDecoration(worldGenRegion, structFeat.forWorldGenRegion(worldGenRegion));
                    //Blender.generateBorderTicks(worldGenRegion, chunk);

                    protoChunk.setStatus(STATUS);
                }
            } finally {
                testLock.unlock();
            }
            return chunk;
        }

        static ChunkAccess load(ChunkAccess chunk) {
            ((ProtoChunk) chunk).setStatus(STATUS);
            return chunk;
        }
    }

    public static class StepLight {
        public static final ChunkStatus STATUS = ChunkStatus.LIGHT;
        public static final int RANGE = STATUS.getRange();
        public static final EnumSet<Heightmap.Types> HEIGHTMAP_TYPES = STATUS.heightmapsAfter();
        private static ThreadedLevelLightEngine lightEngine;

        public final static void onLevelLoad(ThreadedLevelLightEngine engine) {
            lightEngine = engine;
        }

        public static final ChunkAccess generate(ChunkAccess chunk) {
            ((ProtoChunk) chunk).setStatus(STATUS);
            return joinAsync(lightEngine.lightChunk(chunk, chunk.isLightCorrect()));
        }

        public static final ChunkAccess load(ChunkAccess chunk) {
            ((ProtoChunk) chunk).setStatus(STATUS);
            return joinAsync(lightEngine.lightChunk(chunk, chunk.isLightCorrect()));
        }
    }

    // The following may not be needed
    /*
     * public static class Spawn implements SimpleGen {
     *
     * @Override public EnumSet<Types> getHeightmapTypes() { return POST_FEATURES; }
     *
     * @Override public int getDependencyRange() { return 0; }
     *
     * @Override public final void doSimpleWork(ChunkStatus targetStatus,
     * ServerLevel level, ChunkGenerator generator, List<ChunkAccess> chunkList,
     * ChunkAccess chunk) { if (!chunk.isUpgrading())
     * generator.spawnOriginalMobs(new WorldGenRegion(level, chunkList,
     * targetStatus, -1)); } } public static class Heightmaps implements SimpleGen {
     *
     * @Override public EnumSet<Types> getHeightmapTypes() { return POST_FEATURES; }
     *
     * @Override public int getDependencyRange() { return 0; }
     *
     * @Override public final void doSimpleWork(ChunkStatus targetStatus,
     * ServerLevel level, ChunkGenerator generator, List<ChunkAccess> chunkList,
     * ChunkAccess chunk) { // Apearently nothing again??? Decompiler Error? } }
     *
     * public static class Full implements Gen {
     *
     * @Override public EnumSet<Types> getHeightmapTypes() { return POST_FEATURES; }
     *
     * @Override public int getDependencyRange() { return 0; }
     *
     * @Override public final ChunkAccess doWork(ChunkStatus targetStatus, Executor
     * worker, ServerLevel level, ChunkGenerator generator, StructureManager
     * structures, ThreadedLevelLightEngine lightEngine, Mutator function,
     * List<ChunkAccess> chunkList, ChunkAccess chunk, boolean alwaysRegenerate) {
     * return function.call(chunk); }
     *
     * @Override public final ChunkAccess load(ChunkStatus targetStatus, ServerLevel
     * level, StructureManager structures, ThreadedLevelLightEngine lightEngine,
     * Mutator function, ChunkAccess chunk) { return function.call(chunk); } }
     *
     *
     */

}
