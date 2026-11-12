package com.nozz.ambuscade.ledger;

import com.nozz.ambuscade.AmbuscadeRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** A ledger and one book make two of that ledger, the same list included: what crafting_transmute does on newer versions. */
public class LedgerCopyRecipe extends CustomRecipe {
    public LedgerCopyRecipe(CraftingBookCategory category) {
        super(category);
    }

    // the ledger in the grid, or empty unless the grid holds exactly one ledger and one book
    private static ItemStack ledger(CraftingInput input) {
        ItemStack ledger = ItemStack.EMPTY;
        int books = 0;
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) continue;
            if (stack.is(AmbuscadeRegistry.PERIMETER_LEDGER.get()) && ledger.isEmpty()) ledger = stack;
            else if (stack.is(Items.BOOK)) books++;
            else return ItemStack.EMPTY;
        }
        return books == 1 ? ledger : ItemStack.EMPTY;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !ledger(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return ledger(input).copyWithCount(2);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return AmbuscadeRegistry.LEDGER_COPY.get();
    }
}
