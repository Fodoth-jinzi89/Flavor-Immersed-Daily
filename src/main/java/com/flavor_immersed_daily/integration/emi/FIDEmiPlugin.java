package com.flavor_immersed_daily.integration.emi;

import com.flavor_immersed_daily.FlavorImmersedDaily;
import com.flavor_immersed_daily.all.ModBlocks;
import com.flavor_immersed_daily.recipe.EggBreakingRecipe;
import com.flavor_immersed_daily.recipe.FridgeFreezingRecipe;
import com.flavor_immersed_daily.recipe.FridgeTemperingRecipe;
import com.flavor_immersed_daily.recipe.ModRecipes;
import com.flavor_immersed_daily.recipe.WoodBasinRecipe;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

@EmiEntrypoint
public final class FIDEmiPlugin implements EmiPlugin {
    public static final EmiRecipeCategory WOOD_BASIN = category("wood_basin", ModBlocks.WOODBASIN.asItem());
    public static final EmiRecipeCategory EGG_BREAKING = category("egg_breaking", ModBlocks.EGGBREAKINGMACHINE.asItem());
    public static final EmiRecipeCategory FRIDGE_TEMPERING = category("fridge_tempering", ModBlocks.FRIDGE.asItem());
    public static final EmiRecipeCategory FRIDGE_FREEZING = category("fridge_freezing", ModBlocks.FRIDGE.asItem());

    private static EmiRecipeCategory category(String path, net.minecraft.world.level.ItemLike icon) {
        return new EmiRecipeCategory(id(path), EmiStack.of(icon));
    }

    static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(FlavorImmersedDaily.MODID, path);
    }

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(WOOD_BASIN);
        registry.addCategory(EGG_BREAKING);
        registry.addCategory(FRIDGE_TEMPERING);
        registry.addCategory(FRIDGE_FREEZING);

        registry.addWorkstation(WOOD_BASIN, EmiStack.of(ModBlocks.WOODBASIN.asItem()));
        registry.addWorkstation(EGG_BREAKING, EmiStack.of(ModBlocks.EGGBREAKINGMACHINE.asItem()));
        registry.addWorkstation(FRIDGE_TEMPERING, EmiStack.of(ModBlocks.FRIDGE.asItem()));
        registry.addWorkstation(FRIDGE_FREEZING, EmiStack.of(ModBlocks.FRIDGE.asItem()));

        for (RecipeHolder<WoodBasinRecipe> holder : registry.getRecipeManager()
                .getAllRecipesFor(ModRecipes.WOOD_BASIN_TYPE.get())) {
            registry.addRecipe(FIDEmiRecipe.woodBasin(holder));
        }
        for (RecipeHolder<EggBreakingRecipe> holder : registry.getRecipeManager()
                .getAllRecipesFor(ModRecipes.EGG_BREAKING_TYPE.get())) {
            registry.addRecipe(FIDEmiRecipe.eggBreaking(holder));
        }
        for (RecipeHolder<FridgeTemperingRecipe> holder : registry.getRecipeManager()
                .getAllRecipesFor(ModRecipes.FRIDGE_TEMPERING_TYPE.get())) {
            registry.addRecipe(FIDEmiRecipe.tempering(holder));
        }
        for (RecipeHolder<FridgeFreezingRecipe> holder : registry.getRecipeManager()
                .getAllRecipesFor(ModRecipes.FRIDGE_FREEZING_TYPE.get())) {
            registry.addRecipe(FIDEmiRecipe.freezing(holder));
        }
    }
}
