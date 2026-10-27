package com.horrortown.entity;

import com.horrortown.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;

/**
 * What each kind of townsfolk buys and sells, for 铜钱 (copper coins). As the town turns ({@code stage}), what
 * they sell gets dearer.
 */
final class TownTrades {
    private static final int MAX_USES = 12;

    private TownTrades() {
    }

    static MerchantOffers offers(TownRole role, int stage) {
        MerchantOffers offers = new MerchantOffers();
        Trades t = new Trades(offers, 1.0F + stage * 0.5F);
        switch (role) {
            case FISHER -> {
                t.buy(Items.COD, 8, 1);
                t.buy(Items.SALMON, 5, 1);
                t.sell(1, Items.COOKED_COD, 4);
                t.sell(1, Items.DRIED_KELP, 10);
                t.sell(3, Items.FISHING_ROD, 1);
            }
            case SALTER -> {
                t.buy(ModItems.SALT_CRUST.get(), 16, 1);
                t.buy(ModItems.ROT_MUD.get(), 24, 1);
                t.sell(1, Items.BREAD, 3);
                t.sell(1, Items.BAKED_POTATO, 4);
                t.sell(2, Items.BUCKET, 1);
            }
            case NET_MENDER -> {
                t.buy(Items.STRING, 12, 1);
                t.buy(Items.ROTTEN_FLESH, 20, 1);
                t.sell(1, Items.PAPER, 6);
                t.sell(2, Items.LEAD, 1);
                t.sell(3, Items.LEATHER_CHESTPLATE, 1);
            }
            case PORTER -> {
                t.buy(ModItems.ROTWOOD_LOG.get(), 10, 1);
                t.buy(Items.COAL, 10, 1);
                t.sell(1, Items.TORCH, 8);
                t.sell(2, Items.LANTERN, 1);
                t.sell(5, Items.IRON_PICKAXE, 1);
            }
            case KEEPER -> {
                t.buy(Items.BONE, 12, 1);
                t.buy(Items.ROTTEN_FLESH, 16, 1);
                t.sell(1, Items.CANDLE, 2);
                t.sell(6, Items.ENDER_PEARL, 1);
                t.sell(10, Items.GOLDEN_APPLE, 1);
            }
        }
        return offers;
    }

    private record Trades(MerchantOffers offers, float markup) {
        /** They pay one coin for {@code count} of an item. */
        void buy(ItemLike item, int count, int coins) {
            this.offers.add(new MerchantOffer(new ItemCost(item, count), new ItemStack(ModItems.COPPER_COIN.get(), coins),
                    MAX_USES, 0, 0.05F));
        }

        /** They sell {@code count} of an item for some coins (more as the town turns). */
        void sell(int coins, ItemLike item, int count) {
            int price = Math.max(1, Math.round(coins * this.markup));
            this.offers.add(new MerchantOffer(new ItemCost(ModItems.COPPER_COIN.get(), price), new ItemStack(item, count),
                    MAX_USES, 0, 0.05F));
        }
    }
}
