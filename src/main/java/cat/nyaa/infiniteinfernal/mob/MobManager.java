package cat.nyaa.infiniteinfernal.mob;

import cat.nyaa.infiniteinfernal.Config;
import cat.nyaa.infiniteinfernal.I18n;
import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.configs.LevelConfig;
import cat.nyaa.infiniteinfernal.configs.MobConfig;
import cat.nyaa.infiniteinfernal.configs.RegionConfig;
import cat.nyaa.infiniteinfernal.utils.Utils;
import cat.nyaa.infiniteinfernal.utils.WeightedPair;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class MobManager {
    private static MobManager instance;

    Map<UUID, IMob> uuidMap = new LinkedHashMap<>();
    Map<World, List<IMob>> worldMobMap = new LinkedHashMap<>();

    Map<String, MobConfig> nameCfgMap = new LinkedHashMap<>();
    Map<String, MobConfig> idCfgMap = new LinkedHashMap<>();
    Map<Integer, List<MobConfig>> natualSpawnList = new LinkedHashMap<>();

    private MobManager() {

    }

    public static MobManager instance() {
        if (instance == null) {
            synchronized (MobManager.class) {
                if (instance == null) {
                    instance = new MobManager();
                }
            }
        }
        return instance;
    }

    public static void disable() {
        instance = null;
    }

    private IMob spawnMobByConfig(MobConfig config, Location location, Integer level) {
        EntityType entityType = config.type;
        World world = location.getWorld();
        if (world != null) {
            Class<? extends Entity> entityClass = entityType.getEntityClass();
            if (entityClass != null && entityClass.isAssignableFrom(LivingEntity.class)) {
                if (level == null) {
                    List<String> levelStrs = config.spawn.levels;
                    List<Integer> levels = MobConfig.parseLevels(levelStrs);
                    level = Utils.randomPick(levels);
                }
                CustomMob customMob = new CustomMob(config, level);
                LivingEntity spawn = (LivingEntity) world.spawn(location, entityClass);
                customMob.makeInfernal(spawn);
                registerMob(customMob);
                return customMob;
            }
        }
        return null;
    }

    public Collection<IMob> getMobs(){
        return uuidMap.values();
    }

    public IMob spawnMobByName(String name, Location location, Integer level) {
        MobConfig mobConfig = nameCfgMap.get(name);
        return spawnMobByConfig(mobConfig, location, level);
    }

    public IMob spawnMobById(String mobId, Location location, Integer level) {
        MobConfig iMob = idCfgMap.get(mobId);
        if (iMob == null) {
            throw new IllegalArgumentException();
        }
        return spawnMobByConfig(iMob, location, level);
    }

    public IMob natualSpawn(EntityType type, Location location) {
        Config config = InfPlugin.plugin.config();
        List<RegionConfig> regions = config.getRegionsForLocation(location);
        List<WeightedPair<MobConfig, Integer>> spawnConfs = new ArrayList<>();
        if (regions.isEmpty()) {
            return null;
        }
        regions.forEach(regionConfig -> {
            if (regionConfig.mobs.isEmpty()) {
                return;
            }
            regionConfig.mobs.forEach(mobs -> {
                try {
                    String[] split = mobs.split(":");
                    String mobId = split[0];
                    int mobWeight = Integer.parseInt(split[1]);
                    MobConfig iMob = idCfgMap.get(mobId);
                    if (iMob == null) {
                        Bukkit.getLogger().log(Level.SEVERE, I18n.format("error.mob.spawn_no_id", mobs));
                        return;
                    }
                    spawnConfs.add(new WeightedPair<>(iMob, 0, mobWeight));
                } catch (NumberFormatException e) {
                    Bukkit.getLogger().log(Level.SEVERE, I18n.format("error.mob.num_format", mobs));
                } catch (Exception e){
                    Bukkit.getLogger().log(Level.SEVERE, I18n.format("error.mob.bad_config", mobs));
                }
            });
        });

        if (!spawnConfs.isEmpty()) {
            WeightedPair<MobConfig, Integer> selected = Utils.weightedRandomPick(spawnConfs);
            MobConfig mobConfig = selected.getKey();
            List<Integer> possibleLevels = MobConfig.parseLevels(mobConfig.spawn.levels);
            return spawnMobByConfig(mobConfig, location, Utils.randomPick(possibleLevels));
        } else {
            Collection<LevelConfig> values = InfPlugin.plugin.config().levelConfigs.values();
            List<WeightedPair<List<MobConfig>, Integer>> levelCandidates = new ArrayList<>();
            values.forEach(levelConfig -> {
                int from = levelConfig.spawnConfig.from;
                int to = levelConfig.spawnConfig.to;
                int level = levelConfig.level;
                int weight = levelConfig.spawnConfig.weight;
                World world = location.getWorld();
                if (world == null) {
                    throw new IllegalArgumentException();
                }
                double distance = location.distance(world.getSpawnLocation());
                if (distance < from || distance >= to) {
                    return;
                }
                List<MobConfig> collect = natualSpawnList.get(level).stream().parallel()
                        .filter(mobConfig -> mobConfig.type.equals(type))
                        .collect(Collectors.toList());
                if (!collect.isEmpty()) {
                    levelCandidates.add(new WeightedPair<>(collect, level, weight));
                }
            });
            if (!levelCandidates.isEmpty()) {
                WeightedPair<List<MobConfig>, Integer> selected = Utils.weightedRandomPick(levelCandidates);
                Integer level = selected.getValue();
                List<MobConfig> candidates = selected.getKey();
                MobConfig mobConfig = Utils.weightedRandomPick(candidates);
                return spawnMobByConfig(mobConfig, location, level);
            }else {
                return null;
            }
        }
    }

    public void registerMob( IMob mob){
        uuidMap.put(mob.getEntity().getUniqueId(), mob);
        World world = mob.getEntity().getWorld();
        List<IMob> iMobs = worldMobMap.computeIfAbsent(world, world1 -> new ArrayList<>());
        iMobs.add(mob);
    }

    public void removeMob(IMob mob){
        World world = mob.getEntity().getWorld();
        uuidMap.remove(mob.getEntity().getUniqueId());
        List<IMob> iMobs = worldMobMap.computeIfAbsent(world, world1 -> new ArrayList<>());
        iMobs.remove(mob);
    }

    public List<IMob> getMobsInWorld(World world) {
        return worldMobMap.computeIfAbsent(world, world1 -> new ArrayList<>());
    }
}
