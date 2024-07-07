package com.hollingsworth.arsnouveau.common.items;

import com.hollingsworth.arsnouveau.api.item.ICasterTool;
import com.hollingsworth.arsnouveau.api.item.IRadialProvider;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.util.StackUtil;
import com.hollingsworth.arsnouveau.client.gui.book.GuiSpellBook;
import com.hollingsworth.arsnouveau.client.gui.book.InfinityGuiSpellBook;
import com.hollingsworth.arsnouveau.client.gui.radial_menu.GuiRadialMenu;
import com.hollingsworth.arsnouveau.client.gui.radial_menu.RadialMenu;
import com.hollingsworth.arsnouveau.client.gui.radial_menu.RadialMenuSlot;
import com.hollingsworth.arsnouveau.client.gui.utils.RenderUtils;
import com.hollingsworth.arsnouveau.client.registry.ModKeyBindings;
import com.hollingsworth.arsnouveau.client.renderer.item.SpellBookRenderer;
import com.hollingsworth.arsnouveau.common.capability.ANPlayerDataCap;
import com.hollingsworth.arsnouveau.common.capability.IPlayerCap;
import com.hollingsworth.arsnouveau.common.crafting.recipes.IDyeable;
import com.hollingsworth.arsnouveau.common.network.Networking;
import com.hollingsworth.arsnouveau.common.network.PacketSetCasterSlot;
import com.hollingsworth.arsnouveau.setup.config.ServerConfig;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry;
import com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SpellBook extends ModItem implements GeoItem, ICasterTool, IDyeable, IRadialProvider {

    public SpellTier tier;
    AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);

    public SpellBook(SpellTier tier) {
        this(new Item.Properties().stacksTo(1).component(DataComponentRegistry.SPELL_CASTER, new SpellCaster(10)), tier);
    }

    public SpellBook(Properties properties, SpellTier tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        ItemStack stack = playerIn.getItemInHand(handIn);
        if (this != ItemsRegistry.CREATIVE_SPELLBOOK.get()) {
            var iMana = CapabilityRegistry.getMana(playerIn).orElse(null);
            if(iMana != null){
                if (iMana.getBookTier() < this.tier.value) {
                    iMana.setBookTier(this.tier.value);
                }
                IPlayerCap cap = CapabilityRegistry.getPlayerDataCap(playerIn).orElse(new ANPlayerDataCap());
                if (iMana.getGlyphBonus() < cap.getKnownGlyphs().size()) {
                    iMana.setGlyphBonus(cap.getKnownGlyphs().size());
                }
            }
        }
        ISpellCaster caster = getSpellCaster(stack);

        return caster.castSpell(worldIn, playerIn, handIn, Component.translatable("ars_nouveau.invalid_spell"));
    }

    @Override
    public int getUseDuration(ItemStack pStack, LivingEntity p_344979_) {
        return 72000;
    }

    /**
     * returns the action that specifies what animation to play when the items is being used
     */
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader world, BlockPos pos, Player player) {
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(final ItemStack stack, @Nullable final TooltipContext world, final List<Component> tooltip, final TooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        tooltip.add(Component.translatable("ars_nouveau.spell_book.select", KeyMapping.createNameSupplier(ModKeyBindings.OPEN_RADIAL_HUD.getName()).get()));
        tooltip.add(Component.translatable("ars_nouveau.spell_book.craft", KeyMapping.createNameSupplier(ModKeyBindings.OPEN_BOOK.getName()).get()));
        tooltip.add(Component.translatable("tooltip.ars_nouveau.caster_level", getTier().value).setStyle(Style.EMPTY.withColor(ChatFormatting.BLUE)));
    }

    public SpellTier getTier() {
        return this.tier;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return factory;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        super.initializeClient(consumer);
        consumer.accept(new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer = new SpellBookRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void onOpenBookMenuKeyPressed(ItemStack stack, Player player) {
        InteractionHand hand = StackUtil.getBookHand(player);
        if (hand == null) {
            return;
        }
        Minecraft.getInstance().setScreen(ServerConfig.INFINITE_SPELLS.get() ? new InfinityGuiSpellBook(hand) : new GuiSpellBook(hand));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void onRadialKeyPressed(ItemStack stack, Player player) {
        Minecraft.getInstance().setScreen(new GuiRadialMenu<>(getRadialMenuProviderForSpellpart(stack)));
    }

    public RadialMenu<AbstractSpellPart> getRadialMenuProviderForSpellpart(ItemStack itemStack) {
        return new RadialMenu<>((int slot) -> {
            Networking.sendToServer(new PacketSetCasterSlot(slot));
        },
                getRadialMenuSlotsForSpellpart(itemStack),
                RenderUtils::drawSpellPart,
                0);
    }

    public List<RadialMenuSlot<AbstractSpellPart>> getRadialMenuSlotsForSpellpart(ItemStack itemStack) {
        SpellCaster spellCaster = SpellCasterRegistry.from(itemStack);
        List<RadialMenuSlot<AbstractSpellPart>> radialMenuSlots = new ArrayList<>();
        for (int i = 0; i < spellCaster.getMaxSlots(); i++) {
            Spell spell = spellCaster.getSpell(i);
            AbstractSpellPart primaryIcon = null;
            List<AbstractSpellPart> secondaryIcons = new ArrayList<>();
            for (AbstractSpellPart p : spell.recipe()) {
                if (p instanceof AbstractCastMethod) {
                    secondaryIcons.add(p);
                }

                if (p instanceof AbstractEffect) {
                    primaryIcon = p;
                    break;
                }
            }
            radialMenuSlots.add(new RadialMenuSlot<>(spellCaster.getSpellName(i), primaryIcon, secondaryIcons));
        }
        return radialMenuSlots;
    }

    @Override
    public boolean canQuickCast() {
        return true;
    }
}
