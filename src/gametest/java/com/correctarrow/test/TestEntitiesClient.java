package com.correctarrow.test;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.resources.Identifier;
//#if MC >= 26.1
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
//#else
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
//#endif
//#if MC >= 1.21.2
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
//#else
import net.minecraft.client.model.HierarchicalModel;
//#endif

public final class TestEntitiesClient implements ClientModInitializer {
    static final ModelLayerLocation SPINNER_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("correctarrow_test", "spinner"), "main");

    public static final float WHEEL_SPEED = 0.15F;

    @Override
    public void onInitializeClient() {
        //#if MC >= 26.1
        ModelLayerRegistry.registerModelLayer(SPINNER_LAYER, TestEntitiesClient::createSpinnerLayer);
        //#else
        EntityModelLayerRegistry.registerModelLayer(SPINNER_LAYER, TestEntitiesClient::createSpinnerLayer);
        //#endif
        EntityRendererRegistry.register(TestEntities.SPINNER, SpinnerRenderer::new);
        EntityRendererRegistry.register(TestEntities.BLOB, NoopRenderer::new);
        //#if !FABRIC_GAMETEST
        if ("own".equals(System.getProperty("correctarrow.harness"))) {
            OwnHarness.install();
        }
        //#endif
    }

    static LayerDefinition createSpinnerLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("post", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 24.0F, 2.0F), PartPose.ZERO);
        root.addOrReplaceChild("wheel", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -1.0F, -8.0F, 16.0F, 2.0F, 16.0F),
                PartPose.offset(0.0F, -2.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/zombie/zombie.png");

    //#if MC >= 1.21.2
    static final class SpinnerModel extends EntityModel<LivingEntityRenderState> {
        final ModelPart wheel;
        final ModelPart post;

        SpinnerModel(ModelPart root) {
            super(root);
            this.wheel = root.getChild("wheel");
            this.post = root.getChild("post");
        }

        @Override
        public void setupAnim(LivingEntityRenderState state) {
            super.setupAnim(state);
            wheel.yRot = state.ageInTicks * WHEEL_SPEED;
        }
    }

    static final class SpinnerRenderer extends MobRenderer<TestEntities.Dummy, LivingEntityRenderState, SpinnerModel> {
        SpinnerRenderer(EntityRendererProvider.Context context) {
            super(context, new SpinnerModel(context.bakeLayer(SPINNER_LAYER)), 0.4F);
        }

        @Override
        public LivingEntityRenderState createRenderState() {
            return new LivingEntityRenderState();
        }

        @Override
        public Identifier getTextureLocation(LivingEntityRenderState state) {
            return TEXTURE;
        }
    }
    //#else
    static final class SpinnerModel extends HierarchicalModel<TestEntities.Dummy> {
        private final ModelPart root;
        final ModelPart wheel;
        final ModelPart post;

        SpinnerModel(ModelPart root) {
            this.root = root;
            this.wheel = root.getChild("wheel");
            this.post = root.getChild("post");
        }

        @Override
        public ModelPart root() {
            return root;
        }

        @Override
        public void setupAnim(TestEntities.Dummy entity, float walkPos, float walkSpeed, float ageInTicks, float headYaw, float pitch) {
            root.getAllParts().forEach(ModelPart::resetPose);
            wheel.yRot = ageInTicks * WHEEL_SPEED;
        }
    }

    static final class SpinnerRenderer extends MobRenderer<TestEntities.Dummy, SpinnerModel> {
        SpinnerRenderer(EntityRendererProvider.Context context) {
            super(context, new SpinnerModel(context.bakeLayer(SPINNER_LAYER)), 0.4F);
        }

        @Override
        public Identifier getTextureLocation(TestEntities.Dummy entity) {
            return TEXTURE;
        }
    }
    //#endif
}
