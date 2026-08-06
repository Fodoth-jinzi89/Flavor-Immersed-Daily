package com.flavor_immersed_daily.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EggBreakingRecipeSerializer implements RecipeSerializer<EggBreakingRecipe> {

    public static final EggBreakingRecipeSerializer INSTANCE = new EggBreakingRecipeSerializer();

    private static final MapCodec<EggBreakingRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            ShapedRecipePattern.MAP_CODEC.codec().optionalFieldOf("pattern").forGetter(
                    (EggBreakingRecipe r) -> Optional.ofNullable(getPattern(r))),
            Ingredient.CODEC_NONEMPTY.listOf().optionalFieldOf("ingredients").forGetter(
                    (EggBreakingRecipe r) -> Optional.ofNullable(getIngredients(r))),
            ItemStack.STRICT_CODEC.listOf(1, 2).fieldOf("results").forGetter(EggBreakingRecipe::getResults),
            Codec.INT.optionalFieldOf("cookingTime", 100).forGetter(EggBreakingRecipe::getCookingTime),
            Codec.BOOL.optionalFieldOf("shaped", true).forGetter(EggBreakingRecipe::isShaped)
    ).apply(inst, EggBreakingRecipe::new));

    private static ShapedRecipePattern getPattern(EggBreakingRecipe recipe) {
        if (!recipe.isShaped()) return null;
        return createPattern(recipe.getIngredientList());
    }

    private static List<Ingredient> getIngredients(EggBreakingRecipe recipe) {
        if (recipe.isShaped()) return null;
        return recipe.getIngredientList();
    }

    private static final StreamCodec<RegistryFriendlyByteBuf, EggBreakingRecipe> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public EggBreakingRecipe decode(RegistryFriendlyByteBuf buf) {
            boolean shaped = buf.readBoolean();
            Optional<ShapedRecipePattern> pattern = Optional.empty();
            Optional<List<Ingredient>> ingredients = Optional.empty();
            if (shaped) {
                pattern = Optional.of(ShapedRecipePattern.STREAM_CODEC.decode(buf));
            } else {
                int count = buf.readVarInt();
                List<Ingredient> list = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    list.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
                }
                ingredients = Optional.of(list);
            }
            int resultCount = buf.readVarInt();
            List<ItemStack> results = new ArrayList<>();
            for (int i = 0; i < resultCount; i++) {
                results.add(ItemStack.STREAM_CODEC.decode(buf));
            }
            int cookingTime = buf.readVarInt();
            return new EggBreakingRecipe(pattern, ingredients, results, cookingTime, shaped);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, EggBreakingRecipe recipe) {
            buf.writeBoolean(recipe.isShaped());
            if (recipe.isShaped()) {
                ShapedRecipePattern p = createPattern(recipe.getIngredientList());
                ShapedRecipePattern.STREAM_CODEC.encode(buf, p);
            } else {
                List<Ingredient> ings = recipe.getIngredientList();
                buf.writeVarInt(ings.size());
                for (Ingredient ing : ings) {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ing);
                }
            }
            List<ItemStack> results = recipe.getResults();
            buf.writeVarInt(results.size());
            for (ItemStack stack : results) {
                ItemStack.STREAM_CODEC.encode(buf, stack);
            }
            buf.writeVarInt(recipe.getCookingTime());
        }
    };

    private static ShapedRecipePattern createPattern(List<Ingredient> ingredients) {
        if (ingredients.isEmpty()) return null;
        int size = ingredients.size();
        int w = size >= 6 ? 3 : (size >= 4 ? 2 : 1);
        int h = (int) Math.ceil((double) size / w);
        NonNullList<Ingredient> list = NonNullList.withSize(w * h, Ingredient.EMPTY);
        for (int i = 0; i < ingredients.size(); i++) {
            list.set(i, ingredients.get(i));
        }
        return new ShapedRecipePattern(w, h, list, Optional.empty());
    }

    @Override
    public MapCodec<EggBreakingRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, EggBreakingRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
