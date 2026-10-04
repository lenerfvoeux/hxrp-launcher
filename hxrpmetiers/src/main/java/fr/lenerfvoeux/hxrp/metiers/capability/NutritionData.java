package fr.lenerfvoeux.hxrp.metiers.capability;

import net.minecraft.nbt.NBTTagCompound;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Faim et soif sur 60, rang du Gourmet (donné par commande admin, sans XP), et cooldowns par aliment (horodatage de fin). */
public class NutritionData {
    public static final double MAX = 60.0;

    public double faim = MAX;
    public double soif = MAX;
    public int rangGourmet = 0;
    public final Map<String, Long> cooldowns = new HashMap<>();
    public boolean dirty = true;

    public void addFaim(double v) { faim = clamp(faim + v); dirty = true; }
    public void addSoif(double v) { soif = clamp(soif + v); dirty = true; }
    public void setFaim(double v) { faim = clamp(v); dirty = true; }
    public void setSoif(double v) { soif = clamp(v); dirty = true; }

    private static double clamp(double v) { return Math.max(0, Math.min(MAX, v)); }

    public long cooldownLeft(String key, long now) {
        Long end = cooldowns.get(key);
        return end == null ? 0 : Math.max(0, end - now);
    }

    public void setCooldown(String key, long end) { cooldowns.put(key, end); dirty = true; }

    public void purge(long now) {
        for (Iterator<Map.Entry<String, Long>> it = cooldowns.entrySet().iterator(); it.hasNext(); )
            if (it.next().getValue() <= now) { it.remove(); dirty = true; }
    }

    public void copyPersistent(NutritionData o) {
        rangGourmet = o.rangGourmet;
        cooldowns.clear();
        cooldowns.putAll(o.cooldowns);
        dirty = true;
    }

    public NBTTagCompound write() {
        NBTTagCompound t = new NBTTagCompound();
        t.setDouble("faim", faim);
        t.setDouble("soif", soif);
        t.setInteger("rang", rangGourmet);
        NBTTagCompound c = new NBTTagCompound();
        for (Map.Entry<String, Long> e : cooldowns.entrySet()) c.setLong(e.getKey(), e.getValue());
        t.setTag("cooldowns", c);
        return t;
    }

    public void read(NBTTagCompound t) {
        faim = t.hasKey("faim") ? t.getDouble("faim") : MAX;
        soif = t.hasKey("soif") ? t.getDouble("soif") : MAX;
        rangGourmet = t.getInteger("rang");
        cooldowns.clear();
        NBTTagCompound c = t.getCompoundTag("cooldowns");
        for (String k : c.getKeySet()) cooldowns.put(k, c.getLong(k));
        dirty = true;
    }
}
