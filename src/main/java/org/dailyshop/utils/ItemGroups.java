package org.dailyshop.utils;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;

public class ItemGroups {
    /**
     * Définissez ici vos groupes de variantes.
     */
    public static final Map<Material, List<Material>> VARIANTS = Map.of(
            // laine : cycle white → red → blue → green → …
            Material.WHITE_WOOL, List.of(
                    Material.WHITE_WOOL, Material.ORANGE_WOOL, Material.MAGENTA_WOOL,
                    Material.LIGHT_BLUE_WOOL, Material.YELLOW_WOOL, Material.LIME_WOOL,
                    Material.PINK_WOOL, Material.GRAY_WOOL, Material.LIGHT_GRAY_WOOL,
                    Material.CYAN_WOOL, Material.PURPLE_WOOL, Material.BLUE_WOOL,
                    Material.BROWN_WOOL, Material.GREEN_WOOL, Material.RED_WOOL,
                    Material.BLACK_WOOL
            ),
            // froglight
            Material.OCHRE_FROGLIGHT, List.of(
                    Material.OCHRE_FROGLIGHT, Material.VERDANT_FROGLIGHT,
                    Material.PEARLESCENT_FROGLIGHT
            ),

            // eggs
            Material.EGG, List.of(
                    Material.EGG, Material.BROWN_EGG, Material.BLUE_EGG
            )

            // ajoutez d'autres groupes si nécessaire
    );
}
