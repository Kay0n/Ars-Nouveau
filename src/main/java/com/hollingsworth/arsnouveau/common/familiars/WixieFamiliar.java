package com.hollingsworth.arsnouveau.common.familiars;

import com.hollingsworth.arsnouveau.api.familiar.AbstractFamiliarHolder;
import com.hollingsworth.arsnouveau.api.familiar.IFamiliar;
import com.hollingsworth.arsnouveau.common.entity.EntityWixie;
import com.hollingsworth.arsnouveau.common.entity.ModEntities;
import com.hollingsworth.arsnouveau.common.entity.familiar.FamiliarWixie;
import net.minecraft.world.World;

public class WixieFamiliar extends AbstractFamiliarHolder {
    public WixieFamiliar() {
        super("wixie", (e) -> e instanceof EntityWixie);
    }

    @Override
    public IFamiliar getSummonEntity(World world) {
        return new FamiliarWixie(ModEntities.ENTITY_FAMILIAR_WIXIE, world);
    }
}
