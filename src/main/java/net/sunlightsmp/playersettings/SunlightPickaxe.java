package net.sunlightsmp.playersettings;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.util.Vector;

public final class SunlightPickaxe implements Listener {
    private final SunlightPlayerSettings plugin;
    private final NamespacedKey expiryKey;
    private final NamespacedKey pickaxeKey;
    public SunlightPickaxe(SunlightPlayerSettings plugin){
        this.plugin=plugin;
        expiryKey=new NamespacedKey(plugin,"sunlight_pickaxe_expiry");
        pickaxeKey=new NamespacedKey(plugin,"sunlight_pickaxe");
    }

    public ItemStack create(){
        ItemStack item=new ItemStack(Material.NETHERITE_PICKAXE);
        ItemMeta meta=item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD+"☀ Sunlight Pickaxe");
        meta.setLore(List.of(
            ChatColor.YELLOW+"3x3 Mining",
            ChatColor.GRAY+"Breaks a 3x3 area at once.",
            "",
            ChatColor.RED+"Expires in 2 days",
            ChatColor.DARK_GRAY+"Temporary Sunlight tool"
        ));
        meta.getPersistentDataContainer().set(pickaxeKey,PersistentDataType.BYTE,(byte)1);
        meta.getPersistentDataContainer().set(expiryKey,PersistentDataType.LONG,System.currentTimeMillis()+172800000L);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isPickaxe(ItemStack item){
        if(item==null||item.getType()!=Material.NETHERITE_PICKAXE||!item.hasItemMeta())return false;
        Byte marker=item.getItemMeta().getPersistentDataContainer().get(pickaxeKey,PersistentDataType.BYTE);
        return marker!=null&&marker==1;
    }

    private boolean expired(ItemStack item){
        Long time=item.getItemMeta().getPersistentDataContainer().get(expiryKey,PersistentDataType.LONG);
        return time==null||System.currentTimeMillis()>=time;
    }

    private void expire(Player p){
        p.getInventory().setItemInMainHand(null);
        p.sendMessage(ChatColor.RED+"☀ Your Sunlight Pickaxe has expired.");
        p.playSound(p.getLocation(),Sound.ENTITY_ITEM_BREAK,1f,0.8f);
    }

    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true)
    public void onBreak(BlockBreakEvent event){
        Player p=event.getPlayer();
        ItemStack tool=p.getInventory().getItemInMainHand();
        if(!isPickaxe(tool))return;
        if(expired(tool)){event.setCancelled(true);expire(p);return;}

        Block center=event.getBlock();
        Vector dir=p.getLocation().getDirection();
        int minX=center.getX(),maxX=center.getX(),minY=center.getY(),maxY=center.getY(),minZ=center.getZ(),maxZ=center.getZ();

        if(Math.abs(dir.getY())>0.7){
            minX=center.getX()-1; maxX=center.getX()+1;
            minZ=center.getZ()-1; maxZ=center.getZ()+1;
        }else if(Math.abs(dir.getX())>Math.abs(dir.getZ())){
            minY=center.getY()-1; maxY=center.getY()+1;
            minZ=center.getZ()-1; maxZ=center.getZ()+1;
        }else{
            minX=center.getX()-1; maxX=center.getX()+1;
            minY=center.getY()-1; maxY=center.getY()+1;
        }

        List<Block> blocks=new ArrayList<>();
        for(int x=minX;x<=maxX;x++)for(int y=minY;y<=maxY;y++)for(int z=minZ;z<=maxZ;z++){
            Block b=center.getWorld().getBlockAt(x,y,z);
            if(b.getType().isAir()||b.isLiquid()||b.getType()==Material.BEDROCK)continue;
            if(!b.equals(center)&&!b.getType().isOccluding())continue;
            blocks.add(b);
        }
        for(Block b:blocks){
            if(b.equals(center))continue;
            BlockBreakEvent extra=new BlockBreakEvent(b,p);
            plugin.getServer().getPluginManager().callEvent(extra);
            if(extra.isCancelled())continue;
            b.breakNaturally(tool);
        }
        // Let the original event handle the center block, preserving normal drops/XP/protection.
    }
}
