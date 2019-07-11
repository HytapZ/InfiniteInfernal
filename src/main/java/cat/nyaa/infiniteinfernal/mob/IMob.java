package cat.nyaa.infiniteinfernal.mob;

import cat.nyaa.infiniteinfernal.abilitiy.IAbility;
import cat.nyaa.infiniteinfernal.loot.ILootItem;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.List;
import java.util.Map;

public interface IMob {
    Map<ILootItem, Integer> getLoots();
    Map<ILootItem, Integer> getSpecialLoots();
    List<IAbility> getAbilities();
    LivingEntity getEntity();
    EntityType getEntityType();
    KeyedBossBar getBossBar();
    int getLevel();
    double getDamage();
    double getMaxHealth();
    double getSpecialChance();
    void makeInfernal(LivingEntity entity);
    boolean isAutoSpawn();
    boolean dropVanilla();
    String getName();
    String getTaggedName();

    void showParticleEffect();

    void autoRetarget();

    LivingEntity getTarget();
    boolean isTarget(LivingEntity target);
}
