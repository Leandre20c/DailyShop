package org.dailyshop.managers;

import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcData;
import de.oliver.fancynpcs.api.skins.SkinData;
import de.oliver.fancynpcs.api.skins.SkinData.SkinVariant;
import org.dailyshop.DailyShopPlugin;
import org.dailyshop.model.Shop;

public class NPCManager {

    private final DailyShopPlugin plugin;
    private Npc npc;
    private NpcData data;

    public NPCManager(DailyShopPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Charge l'NPC en utilisant soit un ID (si la config est un nombre),
     * soit un nom (si c'est une chaîne non numérique).
     */
    public boolean loadNPC() {
        String ident = plugin.getConfig().getString("npc-name", "").trim();
        if (ident.isEmpty()) {
            plugin.getLogger().warning("Aucun 'npc-name' défini dans la config !");
            return false;
        }

        Npc found = null;
        try {
            int id = Integer.parseInt(ident);
            found = FancyNpcsPlugin.get().getNpcManager().getNpc(id);
        } catch (NumberFormatException ignored) {}

        if (found == null) {
            found = FancyNpcsPlugin.get().getNpcManager().getAllNpcs().stream()
                    .filter(n -> n.getData().getName().equalsIgnoreCase(ident))
                    .findFirst()
                    .orElse(null);
        }

        if (found == null) {
            plugin.getLogger().warning("NPC Fancy introuvable pour 'npc-name': " + ident);
            return false;
        }

        this.npc = found;
        this.data = npc.getData();
        plugin.getLogger().info("✅ NPC chargé : " + data.getName() + " (" + data.getDisplayName() + ")");
        return true;
    }

    /**
     * Met à jour le nom et le skin du NPC selon le shop courant.
     */
    public void applyCurrentShop() {
        if (npc == null) {
            plugin.getLogger().warning("NPC non chargé, impossible d'appliquer le shop.");
            return;
        }

        Shop current = plugin.getShopManager().getCurrentShop();
        if (current == null) {
            plugin.getLogger().warning("Shop actuel introuvable.");
            return;
        }

        data.setDisplayName(current.getName());

        if (current.getSkin() != null && !current.getSkin().isEmpty()) {
            SkinData skin = FancyNpcsPlugin.get().getSkinManager().getByUsername(current.getSkin(), SkinVariant.AUTO);
            if (skin != null) {
                data.setSkinData(skin);
                plugin.getLogger().info("✅ Skin appliqué depuis le pseudo : " + current.getSkin());
            } else {
                plugin.getLogger().warning("❌ Skin introuvable pour le pseudo : " + current.getSkin());
            }
        }

        npc.updateForAll();
        npc.removeForAll();
        npc.spawnForAll();

        plugin.getLogger().info("✅ NPC mis à jour – Shop : " + current.getName());
    }
}
