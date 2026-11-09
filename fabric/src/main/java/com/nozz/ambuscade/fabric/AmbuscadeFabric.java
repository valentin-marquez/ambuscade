package com.nozz.ambuscade.fabric;

import com.nozz.ambuscade.Ambuscade;
import net.fabricmc.api.ModInitializer;

public final class AmbuscadeFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Ambuscade.init();
    }
}
