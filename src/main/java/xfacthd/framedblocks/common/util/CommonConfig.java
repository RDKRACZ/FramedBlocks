package xfacthd.framedblocks.common.util;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.commons.lang3.tuple.Pair;
import xfacthd.framedblocks.FramedBlocks;

public class CommonConfig
{
    public static final ForgeConfigSpec SPEC;
    public static final CommonConfig INSTANCE;

    public static boolean fireproofBlocks = false;

    private final ForgeConfigSpec.BooleanValue fireproofBlocksValue;

    static
    {
        final Pair<CommonConfig, ForgeConfigSpec> configSpecPair = new ForgeConfigSpec.Builder().configure(CommonConfig::new);
        SPEC = configSpecPair.getRight();
        INSTANCE = configSpecPair.getLeft();
    }

    public CommonConfig(ForgeConfigSpec.Builder builder)
    {
        FMLJavaModLoadingContext.get().getModEventBus().register(this);

        builder.push("general");
        fireproofBlocksValue = builder
                .comment("If true, framed blocks are completely fire proof")
                .translation("config." + FramedBlocks.MODID + ".fireproof")
                .define("fireproofBlocks", false);
        builder.pop();
    }

    @SubscribeEvent
    public void onConfigReloaded(ModConfigEvent event)
    {
        if (event.getConfig().getType() == ModConfig.Type.COMMON && event.getConfig().getModId().equals(FramedBlocks.MODID))
        {
            fireproofBlocks = fireproofBlocksValue.get();
        }
    }
}