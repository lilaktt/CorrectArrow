package com.correctarrow.test;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

public final class TestEntities implements ModInitializer {
    public static EntityType<Dummy> SPINNER;

    public static EntityType<Dummy> BLOB;

    @Override
    public void onInitialize() {
        SPINNER = register("spinner", 1.0F, 1.8F);
        BLOB = register("blob", 0.8F, 0.8F);
    }

    private static EntityType<Dummy> register(String name, float width, float height) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("correctarrow_test", name));
        EntityType.Builder<Dummy> builder = EntityType.Builder.of(Dummy::new, MobCategory.MISC).sized(width, height);
        //#if MC >= 1.21.2
        EntityType<Dummy> type = Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
        //#else
        EntityType<Dummy> type = Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(name));
        //#endif
        FabricDefaultAttributeRegistry.register(type, Mob.createMobAttributes());
        return type;
    }

    public static final class Dummy extends PathfinderMob {
        public Dummy(EntityType<? extends PathfinderMob> type, Level level) {
            super(type, level);
        }
    }
}
