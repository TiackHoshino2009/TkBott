package com.tkbott;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public final class TKBott extends JavaPlugin implements Listener, CommandExecutor {
   private static final Material[][] ARMOR;
   private static final String[] ARMOR_NAMES;
   private static final int[] ARMOR_SLOTS;
   private static final Material[] TIER_ICONS;
   private static final int[] TOTEM_STEPS;
   private static final int SLOT_INFO = 4;
   private static final int SLOT_GEAR = 12;
   private static final int SLOT_SWORD = 14;
   private static final int SLOT_TOTEM = 21;
   private static final int SLOT_TIER = 23;
   private static final int SLOT_PVP = 25;
   private static final int SLOT_CONFIRM = 34;
   private static final int SLOT_SPAWN = 48;
   private static final int SLOT_REMOVE = 50;
   private static final long TOTEM_SWAP = 6L;
   private static final double REACH_HIT = (double)3.0F;
   private static final double REACH_PLACE = (double)4.5F;
   private static final double REACH_BREAK = (double)4.5F;
   private static final double REACH_COMBO = (double)4.5F;
   private static final double[] T_SPEED;
   private static final double[] T_CHASE;
   private static final int[] T_ATTACK;
   private static final int[] T_DECISION;
   private static final int[] T_HANDSWAP;
   private static final int[] T_W_CRYSTAL;
   private static final int[] T_W_DTAP;
   private static final int[] T_W_CHAIN;
   private static final int[] T_W_ANCHOR;
   private static final int[] T_CHAINLEN;
   private static final int[] T_SPAM;
   private static final double[] T_MISS;
   private static final int[] T_COOLDOWN;
   private static final int[] T_REACTION;
   private static final int[] T_GAP;
   private static final int[] T_DODGE;
   private static final int[] T_PEARLCD;
   private static final double[] T_COMBO;
   private static final int[] T_STRAFE_MIN;
   private static final int[] T_STRAFE_MAX;
   private static final int[] T_STRAFE_LEVEL;
   private static final double[] T_TAP;
   private static final double[] T_JUMP;
   private static final int[] T_PREDICT;
   private static final boolean[] T_COUNTER;
   private static final boolean[] T_GAPPLE;
   private static final boolean[] T_ADAPT;
   private final Map<UUID, Settings> settings = new ConcurrentHashMap();
   private final Map<UUID, BotState> states = new ConcurrentHashMap();
   private final Map<String, Saved> pending = new ConcurrentHashMap();
   private final Map<UUID, List<UUID>> confirms = new ConcurrentHashMap();
   private NamespacedKey ownerKey;
   private NamespacedKey crystalKey;
   private NamespacedKey confirmKey;
   private File dataFile;

   public void onEnable() {
      this.saveDefaultConfig();
      this.ownerKey = new NamespacedKey(this, "owner");
      this.crystalKey = new NamespacedKey(this, "crystal");
      this.confirmKey = new NamespacedKey(this, "confirm");
      this.dataFile = new File(this.getDataFolder(), "players.yml");
      this.loadSettings();
      Bukkit.getPluginManager().registerEvents(this, this);
      PluginCommand cmd = this.getCommand("bot");
      if (cmd != null) {
         cmd.setExecutor(this);
      }

      Logger var10000 = this.getLogger();
      String var10001 = this.getPluginMeta().getVersion();
      var10000.info("TKBott v" + var10001 + " da bat. Server: " + Bukkit.getVersion());
   }

   public void onDisable() {
      this.saveSettings();

      for(BotState st : this.states.values()) {
         try {
            Entity e = Bukkit.getEntity(st.botId);
            if (e != null) {
               e.remove();
            }
         } catch (Throwable var8) {
         }
      }

      this.states.clear();

      for(List<UUID> ids : this.confirms.values()) {
         for(UUID eid : ids) {
            try {
               Entity en = Bukkit.getEntity(eid);
               if (en != null) {
                  en.remove();
               }
            } catch (Throwable var7) {
            }
         }
      }

      this.confirms.clear();

      for(Saved sv : this.pending.values()) {
         try {
            sv.block().setBlockData(sv.data(), false);
         } catch (Throwable var6) {
         }
      }

      this.pending.clear();
   }

   public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
      if (sender instanceof Player player) {
         this.openMenu(player);
         return true;
      } else {
         sender.sendMessage("Chỉ người chơi mới dùng được lệnh này.");
         return true;
      }
   }

   private Settings settings(UUID id) {
      return (Settings)this.settings.computeIfAbsent(id, (k) -> new Settings());
   }

   private synchronized void loadSettings() {
      if (this.dataFile.exists()) {
         YamlConfiguration y = YamlConfiguration.loadConfiguration(this.dataFile);

         for(String key : y.getKeys(false)) {
            try {
               UUID id = UUID.fromString(key);
               Settings s = new Settings();
               List<Boolean> n = y.getBooleanList(key + ".nether");

               for(int i = 0; i < 4 && i < n.size(); ++i) {
                  s.nether[i] = (Boolean)n.get(i);
               }

               s.sword = y.getBoolean(key + ".sword");
               s.totems = y.getInt(key + ".totems", -1);
               s.tier = Math.max(1, Math.min(5, y.getInt(key + ".tier", 3)));
               s.pvp = y.getBoolean(key + ".pvp");
               this.settings.put(id, s);
            } catch (IllegalArgumentException var8) {
            }
         }

      }
   }

   private synchronized void saveSettings() {
      YamlConfiguration y = new YamlConfiguration();

      for(Map.Entry<UUID, Settings> en : this.settings.entrySet()) {
         String k = ((UUID)en.getKey()).toString();
         Settings s = (Settings)en.getValue();
         List<Boolean> n = new ArrayList();

         for(boolean b : s.nether) {
            n.add(b);
         }

         y.set(k + ".nether", n);
         y.set(k + ".sword", s.sword);
         y.set(k + ".totems", s.totems);
         y.set(k + ".tier", s.tier);
         y.set(k + ".pvp", s.pvp);
      }

      try {
         this.getDataFolder().mkdirs();
         y.save(this.dataFile);
      } catch (IOException ex) {
         this.getLogger().warning("Khong luu duoc players.yml: " + String.valueOf(ex));
      }

   }

   private void openMenu(Player player) {
      MenuHolder holder = new MenuHolder();
      Inventory inv = Bukkit.createInventory(holder, 54, Component.text("TKBott - Practice Bot"));
      holder.inventory = inv;
      this.render(inv, player);
      player.openInventory(inv);
   }

   private ItemStack button(Material mat, String name, NamedTextColor color, String... lore) {
      ItemStack item = new ItemStack(mat);
      ItemMeta meta = item.getItemMeta();
      meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
      List<Component> lines = new ArrayList();

      for(String line : lore) {
         lines.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
      }

      meta.lore(lines);
      meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
      item.setItemMeta(meta);
      return item;
   }

   private static String totemLabel(int totems) {
      return totems < 0 ? "Vĩnh viễn" : String.valueOf(totems);
   }

   private static String tierDesc(Tier tier) {
      String var10000;
      switch (tier.ordinal()) {
         case 0 -> var10000 = "LT1: CPvP trình cao, phối hợp movement, timing và crystal";
         case 1 -> var10000 = "LT2: movement tốt, xử lý nhanh, chiến thuật linh hoạt";
         case 2 -> var10000 = "LT3: combo crystal, giữ khoảng cách, phản công";
         case 3 -> var10000 = "LT4: biết strafe, đặt và phá crystal";
         case 4 -> var10000 = "LT5: người mới, phản xạ và movement cơ bản";
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   private void render(Inventory inv, Player p) {
      Settings s = this.settings(p.getUniqueId());
      Tier tier = TKBott.Tier.of(s.tier);
      ItemStack filler = this.button(Material.GRAY_STAINED_GLASS_PANE, " ", NamedTextColor.GRAY);

      for(int i = 0; i < inv.getSize(); ++i) {
         inv.setItem(i, filler);
      }

      boolean hasBot = this.getBot(p.getUniqueId()) != null;
      Material var10003 = Material.NETHER_STAR;
      NamedTextColor var10005 = NamedTextColor.LIGHT_PURPLE;
      String[] var10006 = new String[4];
      String var10009 = s.pvp ? "PvP với bạn" : "Đi theo và bảo vệ";
      var10006[0] = "Chế độ: " + var10009;
      var10009 = tier.name();
      var10006[1] = "Độ khó: " + var10009;
      var10009 = totemLabel(s.totems);
      var10006[2] = "Totem: " + var10009;
      var10006[3] = "Bot của bạn: " + (hasBot ? "đang hoạt động" : "chưa triệu hồi");
      inv.setItem(4, this.button(var10003, "Practice Bot", var10005, var10006));
      boolean all = true;
      boolean none = true;

      for(int i = 0; i < 4; ++i) {
         boolean n = s.nether[i];
         all &= n;
         none &= !n;
         int var10001 = ARMOR_SLOTS[i];
         var10003 = ARMOR[i][n ? 1 : 0];
         String var10004 = ARMOR_NAMES[i];
         inv.setItem(var10001, this.button(var10003, var10004 + ": " + (n ? "Netherite" : "Kim cương"), n ? NamedTextColor.GRAY : NamedTextColor.AQUA, "Bấm để đổi Kim cương ↔ Netherite", "Blast Protection IV"));
      }

      Material gearIcon = all ? Material.NETHERITE_CHESTPLATE : (none ? Material.DIAMOND_CHESTPLATE : Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
      String gearLabel = all ? "Netherite" : (none ? "Kim cương" : "Tùy chỉnh");
      inv.setItem(12, this.button(gearIcon, "Gear: " + gearLabel, NamedTextColor.GOLD, "Bấm 1 lần để đổi cả bộ", "Kim cương → Netherite → Kim cương"));
      var10003 = s.sword ? Material.NETHERITE_SWORD : Material.DIAMOND_SWORD;
      String var19 = s.sword ? "Netherite" : "Kim cương";
      inv.setItem(14, this.button(var10003, "Kiếm: " + var19, NamedTextColor.AQUA, "Chỉ cầm khi bật PvP", "Chỉ có Knockback I"));
      var10003 = Material.TOTEM_OF_UNDYING;
      var19 = totemLabel(s.totems);
      inv.setItem(21, this.button(var10003, "Totem: " + var19, NamedTextColor.GOLD, "Bấm để đổi: 0, 1, 5, 10, 20, vĩnh viễn", "Hết totem thì bot chết bình thường"));
      var10003 = TIER_ICONS[s.tier - 1];
      var19 = tier.name();
      inv.setItem(23, this.button(var10003, "Độ khó: " + var19, NamedTextColor.YELLOW, tierDesc(tier), "Bấm để đổi, chỉ có tác dụng khi bật PvP"));
      var10003 = s.pvp ? Material.END_CRYSTAL : Material.GRAY_DYE;
      var19 = s.pvp ? "BẬT" : "TẮT";
      inv.setItem(25, this.button(var10003, "PvP với bạn: " + var19, s.pvp ? NamedTextColor.RED : NamedTextColor.GRAY, "Bot strafe, circle strafe, tap, nhảy né", "Bot đặt obsidian và anchor thật bên cạnh bạn", "Dự đoán vị trí, nối crystal, dtap, anchor", "Tránh nguy hiểm bằng pearl, spam khi có lợi thế", "Crystal và anchor nổ phá block thật", "Tự dùng totem, gapple, đổi đồ trên tay", "Bạn thắng hiện Win Bot, bạn thua hiện Lose Bot", "Bật PvP tự đặt 20 totem", "Sau đó đánh vào block Xác nhận để bắt đầu"));
      inv.setItem(34, this.button(Material.EMERALD_BLOCK, "Tạo nút Xác nhận", NamedTextColor.GREEN, "Hiện block Xác nhận trước mặt bạn", "Đánh vào block để đếm ngược 3 giây rồi PvP", s.pvp ? "Sẵn sàng" : "Cần bật PvP trước"));
      inv.setItem(48, this.button(Material.ZOMBIE_HEAD, "Bot Spawn", NamedTextColor.GREEN, "Triệu hồi bot của bạn, bot sẽ phát sáng", "Mỗi người 1 bot"));
      inv.setItem(50, this.button(Material.BARRIER, "Xóa Bot", NamedTextColor.RED, "Xóa bot hiện tại của bạn"));
   }

   private static int indexOf(int[] arr, int v) {
      for(int i = 0; i < arr.length; ++i) {
         if (arr[i] == v) {
            return i;
         }
      }

      return -1;
   }

   private static int nextTotem(int cur) {
      for(int i = 0; i < TOTEM_STEPS.length; ++i) {
         if (TOTEM_STEPS[i] == cur) {
            return TOTEM_STEPS[(i + 1) % TOTEM_STEPS.length];
         }
      }

      return TOTEM_STEPS[0];
   }

   @EventHandler
   public void onMenuClick(InventoryClickEvent e) {
      if (e.getInventory().getHolder() instanceof MenuHolder) {
         e.setCancelled(true);
         HumanEntity var3 = e.getWhoClicked();
         if (var3 instanceof Player) {
            Player p = (Player)var3;
            if (e.getClickedInventory() == e.getInventory()) {
               Settings s = this.settings(p.getUniqueId());
               int slot = e.getSlot();
               boolean changed = true;
               int armorIdx = indexOf(ARMOR_SLOTS, slot);
               if (armorIdx >= 0) {
                  s.nether[armorIdx] = !s.nether[armorIdx];
               } else {
                  switch (slot) {
                     case 12:
                        boolean all = true;

                        for(boolean b : s.nether) {
                           all &= b;
                        }

                        for(int i = 0; i < 4; ++i) {
                           s.nether[i] = !all;
                        }
                        break;
                     case 14:
                        s.sword = !s.sword;
                        break;
                     case 21:
                        s.totems = nextTotem(s.totems);
                        break;
                     case 23:
                        s.tier = s.tier <= 1 ? 5 : s.tier - 1;
                        break;
                     case 25:
                        s.pvp = !s.pvp;
                        if (s.pvp) {
                           s.totems = 20;
                        } else {
                           this.disarm(p.getUniqueId());
                        }
                        break;
                     case 34:
                        changed = false;
                        p.closeInventory();
                        if (!s.pvp) {
                           p.sendMessage(Component.text("Hãy bật PvP trong menu trước.", NamedTextColor.RED));
                        } else if (this.getBot(p.getUniqueId()) == null) {
                           p.sendMessage(Component.text("Hãy triệu hồi bot trước.", NamedTextColor.RED));
                        } else {
                           this.spawnConfirm(p);
                        }
                        break;
                     case 48:
                        changed = false;
                        p.closeInventory();
                        this.spawnBot(p);
                        break;
                     case 50:
                        changed = false;
                        p.closeInventory();
                        this.removeBot(p);
                        break;
                     default:
                        changed = false;
                  }
               }

               if (changed) {
                  this.saveSettings();
                  this.applyLive(p);
                  this.render(e.getInventory(), p);
                  if (slot == 25 && s.pvp && this.getBot(p.getUniqueId()) != null) {
                     p.closeInventory();
                     this.spawnConfirm(p);
                  }
               }

            }
         }
      }
   }

   private void spawnBot(Player owner) {
      UUID id = owner.getUniqueId();
      if (this.getBot(id) != null) {
         owner.sendMessage(Component.text("Bạn đã có 1 bot rồi. Hãy xóa bot cũ trước.", NamedTextColor.RED));
      } else {
         BotState st = new BotState(id);
         Zombie bot = (Zombie)owner.getWorld().spawn(owner.getLocation(), Zombie.class, (z) -> {
            z.setAdult();
            z.customName(Component.text("Bot (" + owner.getName() + ")", NamedTextColor.AQUA));
            z.setCustomNameVisible(true);
            z.setGlowing(true);
            z.setRemoveWhenFarAway(false);
            z.setPersistent(true);
            z.setCanPickupItems(false);
            z.setShouldBurnInDay(false);
            z.getPersistentDataContainer().set(this.ownerKey, PersistentDataType.STRING, id.toString());
         });
         st.botId = bot.getUniqueId();
         this.states.put(id, st);
         this.applyAll(bot, id);
         boolean disguised = this.trySkinDisguise(bot, owner);
         bot.getScheduler().runAtFixedRate(this, (t) -> this.tickBot(t, st, bot), (Runnable)null, 5L, 5L);
         bot.getScheduler().runAtFixedRate(this, (t) -> this.fastTick(t, st, bot), (Runnable)null, 2L, 2L);
         if (this.settings(id).pvp) {
            this.spawnConfirm(owner);
         }

         owner.sendMessage(Component.text("Đã triệu hồi bot của bạn.", NamedTextColor.GREEN));
         if (!disguised) {
            owner.sendMessage(Component.text("Chưa có plugin LibsDisguises nên bot vẫn mang hình zombie, chưa có skin NPC.", NamedTextColor.GRAY));
         }

      }
   }

   private void removeBot(Player owner) {
      Zombie bot = this.getBot(owner.getUniqueId());
      if (bot == null) {
         owner.sendMessage(Component.text("Bạn chưa có bot nào.", NamedTextColor.RED));
      } else {
         this.states.remove(owner.getUniqueId());
         this.removeConfirm(owner.getUniqueId());
         this.removeEntity(bot);
         owner.sendMessage(Component.text("Đã xóa bot của bạn.", NamedTextColor.YELLOW));
      }
   }

   private void removeEntity(Entity e) {
      e.getScheduler().run(this, (t) -> e.remove(), (Runnable)null);
   }

   private Zombie getBot(UUID ownerId) {
      BotState st = (BotState)this.states.get(ownerId);
      if (st != null && st.botId != null) {
         Entity e = Bukkit.getEntity(st.botId);
         if (e instanceof Zombie) {
            Zombie z = (Zombie)e;
            if (z.isValid() && !z.isDead() && this.isBot(z)) {
               return z;
            }
         }

         this.states.remove(ownerId);
         return null;
      } else {
         return null;
      }
   }

   private boolean isBot(Entity e) {
      return e instanceof Zombie && e.getPersistentDataContainer().has(this.ownerKey, PersistentDataType.STRING);
   }

   private UUID ownerOf(Entity e) {
      String s = (String)e.getPersistentDataContainer().get(this.ownerKey, PersistentDataType.STRING);
      return s == null ? null : UUID.fromString(s);
   }

   private void applyLive(Player p) {
      Zombie bot = this.getBot(p.getUniqueId());
      if (bot != null) {
         UUID id = p.getUniqueId();
         bot.getScheduler().run(this, (t) -> this.applyAll(bot, id), (Runnable)null);
      }
   }

   private ItemStack armorPiece(Material mat) {
      ItemStack item = new ItemStack(mat);
      ItemMeta meta = item.getItemMeta();
      meta.addEnchant(Enchantment.BLAST_PROTECTION, 4, true);
      item.setItemMeta(meta);
      return item;
   }

   private ItemStack swordItem(Settings s) {
      ItemStack item = new ItemStack(s.sword ? Material.NETHERITE_SWORD : Material.DIAMOND_SWORD);
      ItemMeta meta = item.getItemMeta();
      meta.addEnchant(Enchantment.KNOCKBACK, 1, true);
      item.setItemMeta(meta);
      return item;
   }

   private ItemStack handItem(Hand h, Settings s) {
      ItemStack var10000;
      switch (h.ordinal()) {
         case 0 -> var10000 = this.swordItem(s);
         case 1 -> var10000 = new ItemStack(Material.END_CRYSTAL);
         case 2 -> var10000 = new ItemStack(Material.RESPAWN_ANCHOR);
         case 3 -> var10000 = new ItemStack(Material.GLOWSTONE);
         case 4 -> var10000 = new ItemStack(Material.GOLDEN_APPLE);
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   private void setHand(Zombie bot, BotState st, Settings s, Hand h) {
      st.hand = h;
      bot.getEquipment().setItemInMainHand(this.handItem(h, s));
   }

   private void equipTotem(Zombie bot) {
      bot.getEquipment().setItemInOffHand(new ItemStack(Material.TOTEM_OF_UNDYING));
   }

   private void applyAll(Zombie bot, UUID ownerId) {
      Settings s = this.settings(ownerId);
      BotState st = (BotState)this.states.get(ownerId);
      EntityEquipment eq = bot.getEquipment();
      eq.setHelmet(this.armorPiece(ARMOR[0][s.nether[0] ? 1 : 0]));
      eq.setChestplate(this.armorPiece(ARMOR[1][s.nether[1] ? 1 : 0]));
      eq.setLeggings(this.armorPiece(ARMOR[2][s.nether[2] ? 1 : 0]));
      eq.setBoots(this.armorPiece(ARMOR[3][s.nether[3] ? 1 : 0]));
      if (s.pvp && st != null) {
         this.setHand(bot, st, s, TKBott.Hand.SWORD);
      } else if (s.pvp) {
         eq.setItemInMainHand(this.swordItem(s));
      } else {
         eq.setItemInMainHand(new ItemStack(Material.AIR));
      }

      if (s.totems == 0) {
         eq.setItemInOffHand(new ItemStack(Material.AIR));
      } else {
         this.equipTotem(bot);
      }

      eq.setHelmetDropChance(0.0F);
      eq.setChestplateDropChance(0.0F);
      eq.setLeggingsDropChance(0.0F);
      eq.setBootsDropChance(0.0F);
      eq.setItemInMainHandDropChance(0.0F);
      eq.setItemInOffHandDropChance(0.0F);
      if (st != null) {
         st.reserve = s.totems <= 0 ? 0 : s.totems - 1;
         st.swapPending = false;
      }

      AttributeInstance speed = bot.getAttribute(Attribute.MOVEMENT_SPEED);
      if (speed != null) {
         speed.setBaseValue(0.23 * (s.pvp ? TKBott.Tier.of(s.tier).chase() : (double)1.0F));
      }

      AttributeInstance dmg = bot.getAttribute(Attribute.ATTACK_DAMAGE);
      if (dmg != null) {
         dmg.setBaseValue(s.pvp ? (double)1.0F : (double)3.0F);
      }

   }

   private boolean trySkinDisguise(Zombie bot, Player owner) {
      if (Bukkit.getPluginManager().getPlugin("LibsDisguises") == null) {
         return false;
      } else {
         String skin = this.getConfig().getString("skin", "TungTungSahur");
         String shown = "Bot (" + owner.getName() + ")";

         try {
            Class<?> api = Class.forName("me.libraryaddict.disguise.DisguiseAPI");
            Class<?> disguiseCls = Class.forName("me.libraryaddict.disguise.disguisetypes.Disguise");
            Class<?> playerCls = Class.forName("me.libraryaddict.disguise.disguisetypes.PlayerDisguise");

            Object disguise;
            try {
               disguise = playerCls.getConstructor(String.class, String.class).newInstance(skin, skin);
            } catch (NoSuchMethodException var12) {
               disguise = playerCls.getConstructor(String.class).newInstance(skin);
            }

            Object watcher = disguiseCls.getMethod("getWatcher").invoke(disguise);

            try {
               watcher.getClass().getMethod("setCustomName", String.class).invoke(watcher, shown);
               watcher.getClass().getMethod("setCustomNameVisible", Boolean.TYPE).invoke(watcher, true);
               watcher.getClass().getMethod("setGlowing", Boolean.TYPE).invoke(watcher, true);
            } catch (NoSuchMethodException var11) {
            }

            api.getMethod("disguiseToAll", Entity.class, disguiseCls).invoke((Object)null, bot, disguise);
            return true;
         } catch (Throwable ex) {
            this.getLogger().warning("Khong the disguise bot bang LibsDisguises: " + String.valueOf(ex));
            return false;
         }
      }
   }

   private void showResult(Player p, boolean win, String name) {
      Title title = win ? Title.title(Component.text("Win Bot", NamedTextColor.GREEN, new TextDecoration[]{TextDecoration.BOLD}), Component.text("Bạn thắng Bot (" + name + ")", NamedTextColor.GRAY), Times.times(Duration.ofMillis(300L), Duration.ofSeconds(3L), Duration.ofMillis(800L))) : Title.title(Component.text("Lose Bot", NamedTextColor.RED, new TextDecoration[]{TextDecoration.BOLD}), Component.text("Bot (" + name + ") thắng bạn", NamedTextColor.GRAY), Times.times(Duration.ofMillis(300L), Duration.ofSeconds(3L), Duration.ofMillis(800L)));
      p.showTitle(title);
   }

   @EventHandler
   public void onDeath(EntityDeathEvent e) {
      if (this.isBot(e.getEntity())) {
         e.getDrops().clear();
         e.setDroppedExp(0);
         UUID ownerId = this.ownerOf(e.getEntity());
         Player owner = Bukkit.getPlayer(ownerId);
         BotState st = (BotState)this.states.get(ownerId);
         if (owner != null && !owner.isDead() && this.settings(ownerId).pvp && st != null && st.armed) {
            this.showResult(owner, true, owner.getName());
         }

         this.removeConfirm(ownerId);
         if (st != null && e.getEntity().getUniqueId().equals(st.botId)) {
            this.states.remove(ownerId);
         }

      }
   }

   @EventHandler
   public void onOwnerDeath(PlayerDeathEvent e) {
      Player p = e.getEntity();
      BotState st = (BotState)this.states.get(p.getUniqueId());
      if (st != null && this.settings(p.getUniqueId()).pvp && st.armed) {
         EntityDamageEvent last = p.getLastDamageCause();
         if (last instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent by = (EntityDamageByEntityEvent)last;
            if (by.getDamager().getUniqueId().equals(st.botId)) {
               this.showResult(p, false, p.getName());
            }
         }

         st.armed = false;
      }
   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onResurrect(EntityResurrectEvent e) {
      LivingEntity var3 = e.getEntity();
      if (var3 instanceof Zombie bot) {
         if (this.isBot(bot)) {
            UUID ownerId = this.ownerOf(bot);
            BotState st = (BotState)this.states.get(ownerId);
            if (st == null) {
               return;
            }

            Settings s = this.settings(ownerId);
            long delay = s.pvp ? 6L : 1L;
            st.swapPending = true;
            bot.getScheduler().runDelayed(this, (t) -> this.reequipTotem(bot, st, s), (Runnable)null, Math.max(1L, delay));
            return;
         }
      }

   }

   private void reequipTotem(Zombie bot, BotState st, Settings s) {
      if (st.swapPending) {
         st.swapPending = false;
         if (s.totems < 0) {
            this.equipTotem(bot);
         } else if (st.reserve > 0) {
            --st.reserve;
            this.equipTotem(bot);
         }

      }
   }

   @EventHandler
   public void onTarget(EntityTargetLivingEntityEvent e) {
      if (this.isBot(e.getEntity()) && e.getTarget() != null) {
         LivingEntity target = e.getTarget();
         if (this.isBot(target)) {
            e.setCancelled(true);
         } else {
            if (target instanceof Player) {
               Player p = (Player)target;
               UUID ownerId = this.ownerOf(e.getEntity());
               BotState bst = (BotState)this.states.get(ownerId);
               boolean allowed = this.settings(ownerId).pvp && bst != null && bst.armed && p.getUniqueId().equals(ownerId);
               if (!allowed) {
                  e.setCancelled(true);
               }
            }

         }
      }
   }

   @EventHandler
   public void onTransform(EntityTransformEvent e) {
      if (this.isBot(e.getEntity())) {
         e.setCancelled(true);
      }

   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onBotHitOwner(EntityDamageByEntityEvent e) {
      Entity var3 = e.getDamager();
      if (var3 instanceof Zombie bot) {
         if (this.isBot(bot)) {
            Entity var4 = e.getEntity();
            if (var4 instanceof Player) {
               Player p = (Player)var4;
               if (p.getUniqueId().equals(this.ownerOf(bot))) {
                  if (e.getCause() != DamageCause.ENTITY_ATTACK) {
                     return;
                  }

                  Settings s = this.settings(p.getUniqueId());
                  BotState st = (BotState)this.states.get(p.getUniqueId());
                  if (s.pvp && st != null && st.armed) {
                     Tier tier = TKBott.Tier.of(s.tier);
                     double chance = tier.combo() * (st.mode == TKBott.Mode.AGGRESSIVE ? 1.4 : (st.mode == TKBott.Mode.DEFENSIVE ? 0.6 : (double)1.0F)) * (st.advantage ? (double)1.5F : (double)1.0F);
                     if (ThreadLocalRandom.current().nextDouble() < chance) {
                        this.startCombo(bot, p, st, tier);
                     }

                     return;
                  }

                  return;
               }
            }

            return;
         }
      }

   }

   @EventHandler(
      ignoreCancelled = true
   )
   public void onBotHurtByOwner(EntityDamageByEntityEvent e) {
      Entity var3 = e.getEntity();
      if (var3 instanceof Zombie bot) {
         if (this.isBot(bot)) {
            UUID ownerId = this.ownerOf(bot);
            Entity damager = e.getDamager();
            if (damager instanceof Projectile) {
               Projectile pr = (Projectile)damager;
               ProjectileSource var7 = pr.getShooter();
               if (var7 instanceof Entity) {
                  Entity shooter = (Entity)var7;
                  damager = shooter;
               }
            }

            if (!damager.getUniqueId().equals(ownerId)) {
               return;
            }

            BotState st = (BotState)this.states.get(ownerId);
            Settings s = this.settings(ownerId);
            if (st != null && s.pvp && st.armed) {
               Tier tier = TKBott.Tier.of(s.tier);
               st.kbTicks = 6;
               if (ThreadLocalRandom.current().nextDouble() < tier.jump()) {
                  st.jumpNow = true;
               }

               if (tier.counter()) {
                  st.counterTicks = 16;
                  st.attackCd = Math.min(st.attackCd, 2);
               }

               return;
            }

            return;
         }
      }

   }

   @EventHandler
   public void onQuit(PlayerQuitEvent e) {
      UUID id = e.getPlayer().getUniqueId();
      Zombie bot = this.getBot(id);
      this.states.remove(id);
      this.removeConfirm(id);
      if (bot != null) {
         this.removeEntity(bot);
      }

   }

   private void disarm(UUID ownerId) {
      BotState st = (BotState)this.states.get(ownerId);
      if (st != null) {
         st.armed = false;
         st.countingDown = false;
         st.control = false;
      }

      this.removeConfirm(ownerId);
   }

   private void removeConfirm(UUID ownerId) {
      List<UUID> ids = (List)this.confirms.remove(ownerId);
      if (ids != null) {
         for(UUID eid : ids) {
            Entity en = Bukkit.getEntity(eid);
            if (en != null) {
               this.removeEntity(en);
            }
         }

      }
   }

   private void spawnConfirm(Player owner) {
      UUID id = owner.getUniqueId();
      BotState st = (BotState)this.states.get(id);
      if (st != null && this.settings(id).pvp) {
         this.removeConfirm(id);
         st.armed = false;
         st.countingDown = false;
         Vector dir = owner.getLocation().getDirection();
         dir.setY(0);
         if (dir.lengthSquared() < 0.01) {
            dir = new Vector(0, 0, 1);
         }

         dir.normalize().multiply(3);
         Location base = owner.getLocation().clone().add(dir);
         base.setY(owner.getLocation().getY() + 1.6);
         World w = owner.getWorld();
         BlockDisplay block = (BlockDisplay)w.spawn(base, BlockDisplay.class, (d) -> {
            d.setBlock(Material.EMERALD_BLOCK.createBlockData());
            d.setTransformation(new Transformation(new Vector3f(-0.4F, 0.0F, -0.4F), new AxisAngle4f(), new Vector3f(0.8F, 0.8F, 0.8F), new AxisAngle4f()));
            d.setGlowing(true);
            d.setGlowColorOverride(Color.LIME);
            d.setPersistent(false);
            d.setVisibleByDefault(false);
         });
         TextDisplay text = (TextDisplay)w.spawn(base.clone().add((double)0.0F, -0.45, (double)0.0F), TextDisplay.class, (d) -> {
            d.text(((TextComponent)Component.text("Xác nhận", NamedTextColor.GREEN, new TextDecoration[]{TextDecoration.BOLD}).append(Component.newline())).append(Component.text("Đánh vào block để bắt đầu PvP", NamedTextColor.GRAY)));
            d.setBillboard(Billboard.CENTER);
            d.setPersistent(false);
            d.setVisibleByDefault(false);
         });
         ArmorStand hit = (ArmorStand)w.spawn(base.clone().add((double)0.0F, -0.9, (double)0.0F), ArmorStand.class, (a) -> {
            a.setInvisible(true);
            a.setGravity(false);
            a.setBasePlate(false);
            a.setArms(false);
            a.setSilent(true);
            a.setCollidable(false);
            a.setPersistent(false);
            a.setVisibleByDefault(false);
            a.getPersistentDataContainer().set(this.confirmKey, PersistentDataType.STRING, id.toString());
         });
         owner.showEntity(this, block);
         owner.showEntity(this, text);
         owner.showEntity(this, hit);
         List<UUID> ids = new ArrayList();
         ids.add(block.getUniqueId());
         ids.add(text.getUniqueId());
         ids.add(hit.getUniqueId());
         this.confirms.put(id, ids);
         hit.getScheduler().runDelayed(this, (t) -> {
            List<UUID> cur = (List)this.confirms.get(id);
            if (cur != null && cur.contains(hit.getUniqueId())) {
               this.removeConfirm(id);
            }

         }, (Runnable)null, 600L);
         owner.sendMessage(Component.text("Đánh vào block Xác nhận trước mặt để bắt đầu PvP. Nút tự mất sau 30 giây.", NamedTextColor.GREEN));
      }
   }

   private void startCountdown(Player p) {
      UUID id = p.getUniqueId();
      BotState st = (BotState)this.states.get(id);
      if (st != null && this.settings(id).pvp && !st.countingDown && !st.armed) {
         st.countingDown = true;
         this.removeConfirm(id);
         this.countdownStep(p, st, 3);
      }
   }

   private void countdownStep(Player p, BotState st, int n) {
      if (p.isOnline() && this.states.get(st.owner) == st && this.settings(st.owner).pvp) {
         if (n > 0) {
            NamedTextColor color = n == 3 ? NamedTextColor.RED : (n == 2 ? NamedTextColor.GOLD : NamedTextColor.YELLOW);
            p.showTitle(Title.title(Component.text(String.valueOf(n), color, new TextDecoration[]{TextDecoration.BOLD}), Component.text("PvP với Bot sắp bắt đầu", NamedTextColor.GRAY), Times.times(Duration.ZERO, Duration.ofMillis(1100L), Duration.ZERO)));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F);
            p.getScheduler().runDelayed(this, (t) -> this.countdownStep(p, st, n - 1), (Runnable)null, 20L);
         } else {
            st.countingDown = false;
            st.armed = true;
            p.showTitle(Title.title(Component.text("FIGHT", NamedTextColor.GREEN, new TextDecoration[]{TextDecoration.BOLD}), Component.text("Đánh bại Bot (" + p.getName() + ")", NamedTextColor.GRAY), Times.times(Duration.ZERO, Duration.ofSeconds(1L), Duration.ofMillis(300L))));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
         }

      } else {
         st.countingDown = false;
      }
   }

   @EventHandler
   public void onConfirmDamage(EntityDamageEvent e) {
      Entity var3 = e.getEntity();
      if (var3 instanceof ArmorStand as) {
         String tag = (String)as.getPersistentDataContainer().get(this.confirmKey, PersistentDataType.STRING);
         if (tag != null) {
            e.setCancelled(true);
            if (e instanceof EntityDamageByEntityEvent) {
               EntityDamageByEntityEvent by = (EntityDamageByEntityEvent)e;
               Entity var6 = by.getDamager();
               if (var6 instanceof Player) {
                  Player p = (Player)var6;
                  if (p.getUniqueId().toString().equals(tag)) {
                     this.startCountdown(p);
                  }
               }
            }

         }
      }
   }

   @EventHandler
   public void onConfirmManipulate(PlayerArmorStandManipulateEvent e) {
      if (e.getRightClicked().getPersistentDataContainer().has(this.confirmKey, PersistentDataType.STRING)) {
         e.setCancelled(true);
      }

   }

   private void tickBot(ScheduledTask task, BotState st, Zombie bot) {
      try {
         if (!bot.isValid() || bot.isDead() || this.states.get(st.owner) != st) {
            task.cancel();
            return;
         }

         Player owner = Bukkit.getPlayer(st.owner);
         if (owner == null) {
            return;
         }

         Settings s = this.settings(st.owner);
         if (!bot.isPersistent()) {
            bot.setPersistent(true);
         }

         if (!st.swapPending && bot.getEquipment().getItemInOffHand().getType() != Material.TOTEM_OF_UNDYING) {
            if (s.totems < 0) {
               this.equipTotem(bot);
            } else if (st.reserve > 0) {
               --st.reserve;
               this.equipTotem(bot);
            }
         }

         Location bl = bot.getLocation();
         Location ol = owner.getLocation();
         double vanish = (double)this.getConfig().getInt("despawn-distance", 100);
         if (!bl.getWorld().equals(ol.getWorld()) || bl.distanceSquared(ol) > vanish * vanish) {
            this.states.remove(st.owner);
            this.removeConfirm(st.owner);
            this.removeEntity(bot);
            task.cancel();
            owner.sendMessage(Component.text("Bot đã biến mất vì bạn đi quá xa. Mở /bot để triệu hồi lại.", NamedTextColor.YELLOW));
            return;
         }

         if (bl.distanceSquared(ol) > (double)1600.0F) {
            bot.teleportAsync(ol);
            return;
         }

         if (s.pvp) {
            this.pvpTick(bot, owner, st, s, bl, ol);
         } else {
            this.peaceTick(bot, bl, ol);
         }
      } catch (Throwable ex) {
         if (!st.errorLogged) {
            st.errorLogged = true;
            this.getLogger().warning("Loi khi bot hoat dong: " + String.valueOf(ex));
         }
      }

   }

   private void peaceTick(Zombie bot, Location bl, Location ol) {
      if (bot.getTarget() instanceof Player) {
         bot.setTarget((LivingEntity)null);
      }

      if (bot.getTarget() == null || !bot.getTarget().isValid()) {
         Monster nearest = null;
         double best = Double.MAX_VALUE;

         for(Entity near : bot.getNearbyEntities((double)10.0F, (double)5.0F, (double)10.0F)) {
            if (near instanceof Monster) {
               Monster m = (Monster)near;
               if (!this.isBot(m)) {
                  double d = m.getLocation().distanceSquared(bl);
                  if (d < best) {
                     best = d;
                     nearest = m;
                  }
               }
            }
         }

         if (nearest != null) {
            bot.setTarget(nearest);
         } else if (bl.distanceSquared(ol) > (double)36.0F) {
            bot.getPathfinder().moveTo(ol, 1.2);
         }

      }
   }

   private void pvpTick(Zombie bot, Player owner, BotState st, Settings s, Location bl, Location ol) {
      GameMode gm = owner.getGameMode();
      if (!owner.isDead() && gm != GameMode.CREATIVE && gm != GameMode.SPECTATOR) {
         if (!st.armed) {
            this.peaceTick(bot, bl, ol);
         } else {
            if (!st.control) {
               bot.setTarget(owner);
            }

         }
      } else {
         bot.setTarget((LivingEntity)null);
         if (bl.distanceSquared(ol) > (double)36.0F) {
            bot.getPathfinder().moveTo(ol, 1.2);
         }

      }
   }

   private void assess(Zombie bot, Player owner, BotState st, Settings s, Tier tier, double dist) {
      AttributeInstance mh = bot.getAttribute(Attribute.MAX_HEALTH);
      double max = mh != null ? mh.getValue() : (double)20.0F;
      double frac = bot.getHealth() / max;
      int botTotems = s.totems < 0 ? 99 : st.reserve + (bot.getEquipment().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING ? 1 : 0);
      double ownerHp = owner.getHealth() + owner.getAbsorptionAmount();
      int ownerTotems = countTotems(owner);
      Mode m = TKBott.Mode.NEUTRAL;
      if (frac < 0.45 && botTotems == 0) {
         m = TKBott.Mode.DEFENSIVE;
      } else if (ownerHp <= (double)8.0F && ownerTotems == 0) {
         m = TKBott.Mode.AGGRESSIVE;
      }

      st.advantage = ownerHp <= (double)10.0F && ownerTotems <= 1 || botTotems > ownerTotems + 1 && frac > (double)0.5F;
      st.mode = m;
      if (tier.gapple() && frac < (double)0.5F && st.gappleCd <= 0 && st.eatLeft <= 0 && st.comboTicks <= 0 && dist > (double)3.5F) {
         st.eatLeft = 32;
         this.setHand(bot, st, s, TKBott.Hand.FOOD);
      }

   }

   private static int countTotems(Player p) {
      int n = 0;

      for(ItemStack it : p.getInventory().getContents()) {
         if (it != null && it.getType() == Material.TOTEM_OF_UNDYING) {
            n += it.getAmount();
         }
      }

      return n;
   }

   private void finishEating(Zombie bot, BotState st, Settings s) {
      bot.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 0));
      bot.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
      AttributeInstance mh = bot.getAttribute(Attribute.MAX_HEALTH);
      double max = mh != null ? mh.getValue() : (double)20.0F;
      bot.setHealth(Math.min(max, bot.getHealth() + (double)4.0F));
      st.gappleCd = 600;
      this.setHand(bot, st, s, TKBott.Hand.SWORD);
      st.handSwapLeft = TKBott.Tier.of(s.tier).handSwap();
   }

   private void checkArmor(Zombie bot, Settings s) {
      EntityEquipment eq = bot.getEquipment();
      ItemStack[] cur = new ItemStack[]{eq.getHelmet(), eq.getChestplate(), eq.getLeggings(), eq.getBoots()};

      for(int i = 0; i < 4; ++i) {
         ItemStack it = cur[i];
         boolean worn = it == null || it.getType() == Material.AIR;
         if (!worn) {
            ItemMeta var9 = it.getItemMeta();
            if (var9 instanceof Damageable) {
               Damageable d = (Damageable)var9;
               int max = it.getType().getMaxDurability();
               worn = max > 0 && (double)d.getDamage() > (double)max * 0.85;
            }
         }

         if (worn) {
            ItemStack fresh = this.armorPiece(ARMOR[i][s.nether[i] ? 1 : 0]);
            switch (i) {
               case 0:
                  eq.setHelmet(fresh);
                  break;
               case 1:
                  eq.setChestplate(fresh);
                  break;
               case 2:
                  eq.setLeggings(fresh);
                  break;
               default:
                  eq.setBoots(fresh);
            }
         }
      }

   }

   private void throwPearl(Zombie bot, Location target) {
      Location eye = bot.getEyeLocation();
      Vector v = target.toVector().subtract(eye.toVector());
      double d = v.length();
      v.normalize().multiply((double)1.5F);
      v.setY(v.getY() + Math.min((double)0.5F, d * 0.015));
      bot.launchProjectile(EnderPearl.class, v);
      bot.getWorld().playSound(eye, Sound.ENTITY_ENDER_PEARL_THROW, 1.0F, 1.0F);
   }

   private static double clamp(double v, double lo, double hi) {
      return Math.max(lo, Math.min(hi, v));
   }

   private boolean canSee(Location from, Location to) {
      Vector d = to.toVector().subtract(from.toVector());
      double len = d.length();
      if (len < (double)0.5F) {
         return true;
      } else {
         RayTraceResult r = from.getWorld().rayTraceBlocks(from, d.normalize(), len - 0.3, FluidCollisionMode.NEVER, true);
         return r == null;
      }
   }

   private boolean canHit(Zombie bot, Player owner) {
      Location eye = bot.getEyeLocation();
      BoundingBox bb = owner.getBoundingBox();
      double cx = clamp(eye.getX(), bb.getMinX(), bb.getMaxX());
      double cy = clamp(eye.getY(), bb.getMinY(), bb.getMaxY());
      double cz = clamp(eye.getZ(), bb.getMinZ(), bb.getMaxZ());
      double dx = eye.getX() - cx;
      double dy = eye.getY() - cy;
      double dz = eye.getZ() - cz;
      if (dx * dx + dy * dy + dz * dz > (double)9.0F) {
         return false;
      } else {
         Location center = new Location(eye.getWorld(), bb.getCenterX(), bb.getCenterY(), bb.getCenterZ());
         return this.canSee(eye, center);
      }
   }

   private boolean reachable(Location eye, Location spot, boolean place) {
      Location aim = crystalLoc(spot, place).clone().add((double)0.0F, (double)1.0F, (double)0.0F);
      return eye.getWorld().equals(aim.getWorld()) && eye.distanceSquared(aim) <= (double)20.25F && this.canSee(eye, aim);
   }

   private boolean crystalNear(Zombie bot) {
      for(Entity en : bot.getNearbyEntities((double)5.0F, (double)3.0F, (double)5.0F)) {
         if (en instanceof EnderCrystal && !en.getPersistentDataContainer().has(this.crystalKey, PersistentDataType.BYTE)) {
            return true;
         }
      }

      return false;
   }

   private void think(Zombie bot, Player owner, BotState st, Settings s, Tier tier, double dist) {
      this.assess(bot, owner, st, s, tier, dist);
      if (st.pearlCooldown <= 0 && !st.pearlPending) {
         boolean danger = st.mode == TKBott.Mode.DEFENSIVE || this.crystalNear(bot);
         if (danger && dist < (double)9.0F) {
            this.schedulePearl(bot, st, tier, true);
            return;
         }

         if (dist > (double)12.0F && dist < (double)32.0F && bot.hasLineOfSight(owner)) {
            this.schedulePearl(bot, st, tier, false);
            return;
         }
      }

      double chance = tier.combo() * (double)0.5F * ((double)tier.decision() / (double)10.0F) * (st.mode == TKBott.Mode.AGGRESSIVE ? (double)1.5F : (st.mode == TKBott.Mode.DEFENSIVE ? (double)0.5F : (double)1.0F)) * (st.advantage ? (double)1.5F : (double)1.0F);
      if (dist <= (double)4.5F && ThreadLocalRandom.current().nextDouble() < chance) {
         this.startCombo(bot, owner, st, tier);
      }

   }

   private void schedulePearl(Zombie bot, BotState st, Tier tier, boolean escape) {
      st.pearlPending = true;
      st.pearlCooldown = tier.pearlCd();
      bot.getScheduler().runDelayed(this, (t) -> {
         st.pearlPending = false;
         if (bot.isValid() && !bot.isDead() && st.armed) {
            Player o = Bukkit.getPlayer(st.owner);
            if (o != null) {
               Location bl = bot.getLocation();
               Location ol = o.getLocation();
               if (bl.getWorld().equals(ol.getWorld())) {
                  Location target = ol;
                  if (escape) {
                     Vector away = bl.toVector().subtract(ol.toVector());
                     away.setY(0);
                     if (away.lengthSquared() < 0.01) {
                        away = new Vector(1, 0, 0);
                     }

                     away.normalize().multiply(14);
                     target = bl.clone().add(away);
                  }

                  this.throwPearl(bot, target);
               }
            }
         }
      }, (Runnable)null, Math.max(1L, (long)tier.dodge()));
   }

   private void trackOwner(BotState st, Location ol) {
      Vector pos = ol.toVector();
      if (st.lastOwnerPos != null) {
         Vector d = pos.clone().subtract(st.lastOwnerPos).multiply((double)0.5F);
         if (d.length() > (double)2.0F) {
            st.ownerVel = new Vector();
         } else {
            st.ownerVel = st.ownerVel.clone().multiply(0.6).add(d.multiply(0.4));
         }
      }

      st.lastOwnerPos = pos;
   }

   private void face(Zombie bot, Location bl, Location ol, double flat) {
      double dx = ol.getX() - bl.getX();
      double dz = ol.getZ() - bl.getZ();
      float yaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
      float pitch = (float)(-Math.toDegrees(Math.atan2(ol.getY() + (double)1.0F - (bl.getY() + 1.6), Math.max(0.1, flat))));
      bot.setRotation(yaw, pitch);
   }

   private void updateStrafe(BotState st, Tier tier) {
      if (tier.strafeLevel() != 0) {
         if (st.pauseLeft > 0) {
            st.pauseLeft -= 2;
         } else {
            st.strafeLeft -= 2;
            if (st.strafeLeft <= 0) {
               ThreadLocalRandom r = ThreadLocalRandom.current();
               st.strafeLeft = r.nextInt(tier.strafeMin(), tier.strafeMax() + 1);
               if (r.nextDouble() < 0.7) {
                  st.strafeDir = -st.strafeDir;
               }

               if (tier.strafeLevel() >= 3 && r.nextDouble() < 0.2) {
                  st.pauseLeft = r.nextInt(2, 7);
               }
            }

         }
      }
   }

   private void fastTick(ScheduledTask task, BotState st, Zombie bot) {
      try {
         if (!bot.isValid() || bot.isDead() || this.states.get(st.owner) != st) {
            task.cancel();
            return;
         }

         Player owner = Bukkit.getPlayer(st.owner);
         Settings s = this.settings(st.owner);
         if (owner == null || !s.pvp || !st.armed) {
            st.control = false;
            return;
         }

         GameMode gm = owner.getGameMode();
         if (owner.isDead() || gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR) {
            st.control = false;
            return;
         }

         Location bl = bot.getLocation();
         Location ol = owner.getLocation();
         if (!bl.getWorld().equals(ol.getWorld())) {
            st.control = false;
            return;
         }

         Tier tier = TKBott.Tier.of(s.tier);
         this.trackOwner(st, ol);
         if (st.attackCd > 0) {
            st.attackCd -= 2;
         }

         if (st.handSwapLeft > 0) {
            st.handSwapLeft -= 2;
         }

         if (st.backTicks > 0) {
            st.backTicks -= 2;
         }

         if (st.kbTicks > 0) {
            st.kbTicks -= 2;
         }

         if (st.counterTicks > 0) {
            st.counterTicks -= 2;
         }

         if (st.comboTicks > 0) {
            st.comboTicks -= 2;
         }

         if (st.gappleCd > 0) {
            st.gappleCd -= 2;
         }

         if (st.cooldown > 0) {
            st.cooldown -= 2;
         }

         if (st.pearlCooldown > 0) {
            st.pearlCooldown -= 2;
         }

         st.armorCheck += 2;
         if (st.armorCheck >= 100) {
            st.armorCheck = 0;
            this.checkArmor(bot, s);
         }

         if (st.eatLeft > 0) {
            st.eatLeft -= 2;
            if (st.eatLeft <= 0) {
               this.finishEating(bot, st, s);
            }
         }

         if (st.comboTicks <= 0 && st.eatLeft <= 0 && st.hand != TKBott.Hand.SWORD) {
            this.setHand(bot, st, s, TKBott.Hand.SWORD);
            st.handSwapLeft = tier.handSwap();
         }

         double dx = ol.getX() - bl.getX();
         double dz = ol.getZ() - bl.getZ();
         double flat = Math.sqrt(dx * dx + dz * dz);
         double dy = ol.getY() - bl.getY();
         boolean los = bot.hasLineOfSight(owner);
         if (!st.control && flat < (double)4.5F && Math.abs(dy) < (double)2.5F && los) {
            st.control = true;
         } else if (st.control && (flat > (double)7.0F || Math.abs(dy) > (double)4.0F || !los)) {
            st.control = false;
         }

         st.sinceDecision += 2;
         if (st.sinceDecision >= tier.decision()) {
            st.sinceDecision = 0;
            this.think(bot, owner, st, s, tier, bl.distance(ol));
         }

         if (!st.control) {
            return;
         }

         bot.setTarget((LivingEntity)null);
         bot.getPathfinder().stopPathfinding();
         this.face(bot, bl, ol, flat);
         this.updateStrafe(st, tier);
         double want = (double)2.5F;
         if (st.backTicks > 0) {
            want = 3.7;
         } else if (st.mode == TKBott.Mode.DEFENSIVE) {
            want = (double)5.0F;
         } else if (st.comboTicks > 0) {
            want = 3.8;
         } else if (st.mode == TKBott.Mode.AGGRESSIVE) {
            want = 2.3;
         }

         double radial = clamp((flat - want) * 0.8, (double)-1.0F, (double)1.0F);
         double ux = flat > 0.01 ? dx / flat : (double)0.0F;
         double uz = flat > 0.01 ? dz / flat : (double)0.0F;
         double px = -uz;
         double var10000;
         switch (tier.strafeLevel()) {
            case 0 -> var10000 = (double)0.0F;
            case 1 -> var10000 = 0.6;
            case 2 -> var10000 = 0.8;
            default -> var10000 = (double)1.0F;
         }

         double amt = var10000;
         if (st.pauseLeft > 0) {
            amt = (double)0.0F;
         }

         double vx = ux * radial + px * (double)st.strafeDir * amt;
         double vz = uz * radial + ux * (double)st.strafeDir * amt;
         double len = Math.sqrt(vx * vx + vz * vz);
         if (len > (double)1.0F) {
            vx /= len;
            vz /= len;
         }

         double sp = tier.speed() * (st.eatLeft > 0 ? 0.4 : (double)1.0F);
         boolean ground = bot.isOnGround();
         double vy = bot.getVelocity().getY();
         if (st.jumpNow && ground) {
            vy = 0.42;
            st.jumpNow = false;
         }

         if (st.kbTicks <= 0 && (ground || vy > 0.3)) {
            bot.setVelocity(new Vector(vx * sp, vy, vz * sp));
         }

         double dist3 = bl.distance(ol);
         boolean canMelee = st.eatLeft <= 0 && st.comboTicks <= 0 && st.hand == TKBott.Hand.SWORD && st.handSwapLeft <= 0;
         if (st.mode == TKBott.Mode.DEFENSIVE && st.counterTicks <= 0 && dist3 > 2.2) {
            canMelee = false;
         }

         if (canMelee && st.attackCd <= 0 && this.canHit(bot, owner)) {
            bot.swingMainHand();
            bot.attack(owner);
            st.attackCd = tier.attack() + ThreadLocalRandom.current().nextInt(0, 3);
            if (ThreadLocalRandom.current().nextDouble() < tier.tap()) {
               st.backTicks = 6;
            }
         }
      } catch (Throwable ex) {
         if (!st.errorLogged) {
            st.errorLogged = true;
            this.getLogger().warning("Loi khi bot di chuyen: " + String.valueOf(ex));
         }
      }

   }

   private Location predicted(Location ol, BotState st, Tier tier) {
      if (tier.predict() <= 0) {
         return ol;
      } else {
         int lead = (int)Math.round((double)(tier.predict() + tier.reaction()) * st.lead);
         Vector off = st.ownerVel.clone().multiply(lead);
         off.setY(0);
         if (off.length() > (double)5.0F) {
            off.normalize().multiply(5);
         }

         return ol.clone().add(off);
      }
   }

   private void startCombo(Zombie bot, Player owner, BotState st, Tier tier) {
      if (st.armed && st.cooldown <= 0 && st.eatLeft <= 0) {
         Location bl = bot.getLocation();
         Location ol = owner.getLocation();
         if (bl.getWorld().equals(ol.getWorld()) && !(bl.distanceSquared(ol) > (double)20.25F) && bot.hasLineOfSight(owner)) {
            boolean anchors = this.getConfig().getBoolean("anchors", true);
            int wc = tier.wCrystal();
            int wd = tier.wDtap();
            int wh = tier.wChain();
            int wa = anchors ? tier.wAnchor() : 0;
            if (st.mode == TKBott.Mode.DEFENSIVE) {
               wd = 0;
               wh = 0;
            }

            if (st.advantage) {
               wd *= 2;
               wh *= 2;
            }

            int total = wc + wd + wh + wa;
            if (total > 0) {
               ThreadLocalRandom r = ThreadLocalRandom.current();
               if (r.nextDouble() < tier.miss()) {
                  st.cooldown = tier.cooldown() / 2;
               } else {
                  boolean place = this.getConfig().getBoolean("place-blocks", true);
                  Location target = this.predicted(ol, st, tier);
                  int pick = r.nextInt(total);
                  int kind = pick < wc ? 0 : (pick < wc + wd ? 1 : (pick < wc + wd + wh ? 2 : 3));
                  int bonus = st.advantage ? 1 : 0;
                  int count = kind == 0 ? 1 : (kind == 1 ? 2 : (kind == 2 ? Math.min(4, tier.chainLen() + bonus) : Math.min(5, tier.spam() + bonus)));
                  Location eye = bot.getEyeLocation();
                  List<Location> spots = new ArrayList();
                  if (kind == 2) {
                     double[] rots = new double[]{(double)45.0F, (double)-45.0F, (double)90.0F, (double)-90.0F};

                     for(int i = 0; i < count && i < rots.length; ++i) {
                        Location sp = this.pickSpot(bl, target, place, rots[i]);
                        if (sp != null && !spots.contains(sp) && this.reachable(eye, sp, place)) {
                           spots.add(sp);
                        }
                     }
                  } else {
                     double side = r.nextBoolean() ? (double)1.0F : (double)-1.0F;
                     Location sp = this.pickSpot(bl, target, place, side * (double)(35 + r.nextInt(26)));
                     if (sp != null && this.reachable(eye, sp, place)) {
                        for(int i = 0; i < count; ++i) {
                           spots.add(sp);
                        }
                     }
                  }

                  if (spots.isEmpty()) {
                     st.cooldown = 10;
                  } else {
                     boolean crystal = kind != 3;
                     int gap = kind == 1 ? 10 : tier.gap();
                     int base = tier.cooldown() + tier.reaction() + (spots.size() - 1) * gap + 8;
                     double scale = st.advantage ? (double)0.5F : (st.mode == TKBott.Mode.AGGRESSIVE ? 0.6 : (double)1.0F);
                     st.cooldown = (int)((double)base * scale);
                     this.burst(bot, st, this.settings(st.owner), spots, gap, crystal, tier.reaction(), place);
                  }
               }
            }
         }
      }
   }

   private boolean isFree(Block b) {
      for(Entity en : b.getWorld().getNearbyEntities(BoundingBox.of(b))) {
         if (en instanceof LivingEntity) {
            return false;
         }
      }

      return true;
   }

   private Location pickSpot(Location bl, Location center, boolean place, double rotDeg) {
      Vector dir = bl.toVector().subtract(center.toVector());
      dir.setY(0);
      if (dir.lengthSquared() < 0.01) {
         dir = new Vector(1, 0, 0);
      }

      dir.normalize();
      double rad = Math.toRadians(rotDeg);
      double cs = Math.cos(rad);
      double sn = Math.sin(rad);
      Vector rot = new Vector(dir.getX() * cs - dir.getZ() * sn, (double)0.0F, dir.getX() * sn + dir.getZ() * cs);
      rot.multiply((double)1.5F);
      Block feet = center.clone().add(rot).getBlock();
      if (!feet.isPassable()) {
         feet = center.getBlock();
      }

      Block ground = feet;

      for(int i = 0; i < 3 && ground.isPassable(); ++i) {
         ground = ground.getRelative(BlockFace.DOWN);
      }

      if (ground.isPassable()) {
         return null;
      } else {
         Block air = ground.getRelative(BlockFace.UP);
         if (air.isPassable() && !air.isLiquid() && this.isFree(air)) {
            if (place) {
               Block up1 = air.getRelative(BlockFace.UP);
               Block up2 = up1.getRelative(BlockFace.UP);
               if (!up1.isPassable() || !up2.isPassable()) {
                  return null;
               }
            }

            return air.getLocation().add((double)0.5F, (double)0.0F, (double)0.5F);
         } else {
            return null;
         }
      }
   }

   private static Location crystalLoc(Location spot, boolean place) {
      return place ? spot.clone().add((double)0.0F, (double)1.0F, (double)0.0F) : spot;
   }

   private void later(Location at, int ticks, Runnable r) {
      Bukkit.getRegionScheduler().runDelayed(this, at, (t) -> r.run(), (long)Math.max(1, ticks));
   }

   private void burst(Zombie bot, BotState st, Settings s, List<Location> spots, int gap, boolean crystal, int reaction, boolean place) {
      UUID ownerId = st.owner;
      int hit = 1 + reaction / 3;
      int count = spots.size();
      st.comboTicks = reaction + (count - 1) * gap + 4 + hit + 6;
      this.setHand(bot, st, s, crystal ? TKBott.Hand.CRYSTAL : TKBott.Hand.ANCHOR);
      if (!crystal) {
         bot.getScheduler().runDelayed(this, (t) -> {
            if (st.hand == TKBott.Hand.ANCHOR) {
               this.setHand(bot, st, s, TKBott.Hand.GLOW);
            }

         }, (Runnable)null, Math.max(1L, (long)reaction + 1L));
      }

      for(int i = 0; i < count; ++i) {
         Location spot = (Location)spots.get(i);
         int base = reaction + i * gap;
         if (crystal) {
            if (place) {
               this.later(spot, base, () -> this.placeObsidian(spot));
            }

            this.later(spot, base + 2, () -> this.spawnCrystal(spot, place));
            this.later(spot, base + 2 + hit, () -> this.detonate(bot, ownerId, spot, true, place));
         } else {
            if (place) {
               this.later(spot, base, () -> this.placeAnchor(spot));
            }

            this.later(spot, base + 2, () -> this.chargeAnchor(spot, place));
            this.later(spot, base + 4, () -> this.detonate(bot, ownerId, spot, false, place));
         }
      }

   }

   private static String key(Block b) {
      String var10000 = String.valueOf(b.getWorld().getUID());
      return var10000 + ":" + b.getX() + ":" + b.getY() + ":" + b.getZ();
   }

   private void placeTemp(Block b, BlockData data) {
      if (this.getConfig().getBoolean("restore-placed-blocks", false)) {
         this.pending.putIfAbsent(key(b), new Saved(b, b.getBlockData()));
         Bukkit.getRegionScheduler().runDelayed(this, b.getLocation(), (t) -> this.restoreTemp(b), 60L);
      }

      b.setBlockData(data, false);
   }

   private void restoreTemp(Block b) {
      Saved sv = (Saved)this.pending.remove(key(b));
      if (sv != null) {
         b.setBlockData(sv.data(), false);
      }

   }

   private void placeObsidian(Location spot) {
      Block b = spot.getBlock();
      if (b.isPassable() && !b.isLiquid() && this.isFree(b)) {
         this.placeTemp(b, Material.OBSIDIAN.createBlockData());
         b.getWorld().playSound(b.getLocation(), Sound.BLOCK_STONE_PLACE, 1.0F, 1.0F);
      }
   }

   private void placeAnchor(Location spot) {
      Block b = spot.getBlock();
      if (b.isPassable() && !b.isLiquid() && this.isFree(b)) {
         RespawnAnchor data = (RespawnAnchor)Material.RESPAWN_ANCHOR.createBlockData();
         data.setCharges(0);
         this.placeTemp(b, data);
         b.getWorld().playSound(b.getLocation(), Sound.BLOCK_STONE_PLACE, 1.0F, 1.0F);
      }
   }

   private void chargeAnchor(Location spot, boolean place) {
      Block b = spot.getBlock();
      if (place && b.getType() == Material.RESPAWN_ANCHOR) {
         RespawnAnchor data = (RespawnAnchor)b.getBlockData();
         data.setCharges(1);
         b.setBlockData(data, false);
      }

      this.chargeFx(spot);
   }

   private void spawnCrystal(Location spot, boolean place) {
      if (!place || spot.getBlock().getType() == Material.OBSIDIAN) {
         crystalLoc(spot, place).getWorld().spawn(crystalLoc(spot, place), EnderCrystal.class, (c) -> {
            c.setShowingBottom(false);
            c.setInvulnerable(true);
            c.setPersistent(false);
            c.getPersistentDataContainer().set(this.crystalKey, PersistentDataType.BYTE, (byte)1);
         });
      }
   }

   private void chargeFx(Location spot) {
      World w = spot.getWorld();
      w.playSound(spot, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0F, 1.0F);
      w.spawnParticle(Particle.PORTAL, spot.clone().add((double)0.0F, (double)0.5F, (double)0.0F), 30, 0.3, 0.3, 0.3, (double)0.5F);
   }

   private void detonate(Zombie bot, UUID ownerId, Location spot, boolean crystal, boolean place) {
      World w = spot.getWorld();
      Location center = crystal ? crystalLoc(spot, place) : spot.clone().add((double)0.0F, (double)0.5F, (double)0.0F);
      boolean found = false;
      if (crystal) {
         for(Entity en : w.getNearbyEntities(center, (double)1.5F, (double)1.5F, (double)1.5F)) {
            if (en.getPersistentDataContainer().has(this.crystalKey, PersistentDataType.BYTE)) {
               en.remove();
               found = true;
            }
         }
      } else {
         Block base = spot.getBlock();
         if (place && base.getType() == Material.RESPAWN_ANCHOR) {
            base.setBlockData(Material.AIR.createBlockData(), false);
            found = true;
         }
      }

      if (!place || found) {
         Location eye = bot.getEyeLocation();
         Location aim = center.clone().add((double)0.0F, crystal ? (double)1.0F : (double)0.0F, (double)0.0F);
         if (eye.getWorld().equals(w) && !(eye.distanceSquared(aim) > (double)20.25F) && this.canSee(eye, aim)) {
            Player owner = Bukkit.getPlayer(ownerId);
            BotState st = (BotState)this.states.get(ownerId);
            if (owner != null && st != null && st.armed && bot.isValid() && !owner.isDead() && this.settings(ownerId).pvp) {
               if (owner.getWorld().equals(w) && !(owner.getLocation().distanceSquared(spot) > (double)100.0F)) {
                  for(Player near : spot.getNearbyPlayers((double)8.0F)) {
                     if (!near.getUniqueId().equals(ownerId)) {
                        return;
                     }
                  }

                  boolean breakBlocks = this.getConfig().getBoolean("explosions-break-blocks", true);
                  w.createExplosion(center, crystal ? 6.0F : 5.0F, false, breakBlocks, bot);
                  if (TKBott.Tier.of(this.settings(ownerId).tier).adapt()) {
                     double d = owner.getLocation().distance(center);
                     if (d > (double)4.5F) {
                        st.lead = Math.min(1.6, st.lead + 0.1);
                     } else {
                        st.lead = Math.max(0.8, st.lead - 0.02);
                     }
                  }

               }
            }
         }
      }
   }

   static {
      ARMOR = new Material[][]{{Material.DIAMOND_HELMET, Material.NETHERITE_HELMET}, {Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE}, {Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS}, {Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS}};
      ARMOR_NAMES = new String[]{"Mũ", "Áo", "Quần", "Giày"};
      ARMOR_SLOTS = new int[]{10, 19, 28, 37};
      TIER_ICONS = new Material[]{Material.NETHERITE_INGOT, Material.DIAMOND, Material.GOLD_INGOT, Material.IRON_INGOT, Material.COPPER_INGOT};
      TOTEM_STEPS = new int[]{0, 1, 5, 10, 20, -1};
      T_SPEED = new double[]{0.32, 0.3, 0.28, 0.26, 0.24};
      T_CHASE = new double[]{1.1, 1.05, (double)1.0F, 0.95, 0.9};
      T_ATTACK = new int[]{10, 11, 13, 16, 20};
      T_DECISION = new int[]{2, 4, 6, 10, 16};
      T_HANDSWAP = new int[]{2, 4, 4, 6, 8};
      T_W_CRYSTAL = new int[]{4, 4, 3, 2, 1};
      T_W_DTAP = new int[]{3, 2, 1, 0, 0};
      T_W_CHAIN = new int[]{3, 2, 1, 0, 0};
      T_W_ANCHOR = new int[]{3, 2, 1, 1, 0};
      T_CHAINLEN = new int[]{3, 3, 2, 2, 2};
      T_SPAM = new int[]{3, 2, 2, 1, 1};
      T_MISS = new double[]{0.05, 0.12, 0.2, 0.3, 0.45};
      T_COOLDOWN = new int[]{24, 32, 44, 60, 80};
      T_REACTION = new int[]{2, 3, 5, 8, 12};
      T_GAP = new int[]{6, 7, 8, 9, 10};
      T_DODGE = new int[]{1, 2, 4, 6, 10};
      T_PEARLCD = new int[]{60, 80, 100, 130, 160};
      T_COMBO = new double[]{(double)0.75F, 0.6, 0.45, 0.3, 0.15};
      T_STRAFE_MIN = new int[]{6, 8, 10, 14, 18};
      T_STRAFE_MAX = new int[]{16, 20, 26, 30, 36};
      T_STRAFE_LEVEL = new int[]{3, 3, 2, 1, 0};
      T_TAP = new double[]{0.7, (double)0.5F, 0.3, (double)0.0F, (double)0.0F};
      T_JUMP = new double[]{0.8, 0.6, 0.35, 0.15, (double)0.0F};
      T_PREDICT = new int[]{6, 5, 4, 2, 0};
      T_COUNTER = new boolean[]{true, true, true, false, false};
      T_GAPPLE = new boolean[]{true, true, true, false, false};
      T_ADAPT = new boolean[]{true, true, false, false, false};
   }

   private static enum Tier {
      LT1,
      LT2,
      LT3,
      LT4,
      LT5;

      double speed() {
         return TKBott.T_SPEED[this.ordinal()];
      }

      double chase() {
         return TKBott.T_CHASE[this.ordinal()];
      }

      int attack() {
         return TKBott.T_ATTACK[this.ordinal()];
      }

      int decision() {
         return TKBott.T_DECISION[this.ordinal()];
      }

      int handSwap() {
         return TKBott.T_HANDSWAP[this.ordinal()];
      }

      int gap() {
         return TKBott.T_GAP[this.ordinal()];
      }

      int dodge() {
         return TKBott.T_DODGE[this.ordinal()];
      }

      int pearlCd() {
         return TKBott.T_PEARLCD[this.ordinal()];
      }

      int wCrystal() {
         return TKBott.T_W_CRYSTAL[this.ordinal()];
      }

      int wDtap() {
         return TKBott.T_W_DTAP[this.ordinal()];
      }

      int wChain() {
         return TKBott.T_W_CHAIN[this.ordinal()];
      }

      int wAnchor() {
         return TKBott.T_W_ANCHOR[this.ordinal()];
      }

      int chainLen() {
         return TKBott.T_CHAINLEN[this.ordinal()];
      }

      int spam() {
         return TKBott.T_SPAM[this.ordinal()];
      }

      double miss() {
         return TKBott.T_MISS[this.ordinal()];
      }

      int cooldown() {
         return TKBott.T_COOLDOWN[this.ordinal()];
      }

      int reaction() {
         return TKBott.T_REACTION[this.ordinal()];
      }

      double combo() {
         return TKBott.T_COMBO[this.ordinal()];
      }

      int strafeMin() {
         return TKBott.T_STRAFE_MIN[this.ordinal()];
      }

      int strafeMax() {
         return TKBott.T_STRAFE_MAX[this.ordinal()];
      }

      int strafeLevel() {
         return TKBott.T_STRAFE_LEVEL[this.ordinal()];
      }

      double tap() {
         return TKBott.T_TAP[this.ordinal()];
      }

      double jump() {
         return TKBott.T_JUMP[this.ordinal()];
      }

      int predict() {
         return TKBott.T_PREDICT[this.ordinal()];
      }

      boolean counter() {
         return TKBott.T_COUNTER[this.ordinal()];
      }

      boolean gapple() {
         return TKBott.T_GAPPLE[this.ordinal()];
      }

      boolean adapt() {
         return TKBott.T_ADAPT[this.ordinal()];
      }

      static Tier of(int n) {
         return values()[Math.max(1, Math.min(5, n)) - 1];
      }

      // $FF: synthetic method
      private static Tier[] $values() {
         return new Tier[]{LT1, LT2, LT3, LT4, LT5};
      }
   }

   private static enum Hand {
      SWORD,
      CRYSTAL,
      ANCHOR,
      GLOW,
      FOOD;

      // $FF: synthetic method
      private static Hand[] $values() {
         return new Hand[]{SWORD, CRYSTAL, ANCHOR, GLOW, FOOD};
      }
   }

   private static enum Mode {
      NEUTRAL,
      AGGRESSIVE,
      DEFENSIVE;

      // $FF: synthetic method
      private static Mode[] $values() {
         return new Mode[]{NEUTRAL, AGGRESSIVE, DEFENSIVE};
      }
   }

   private static final class Settings {
      final boolean[] nether = new boolean[4];
      boolean sword;
      int totems = -1;
      int tier = 3;
      boolean pvp;
   }

   private static final class BotState {
      final UUID owner;
      volatile UUID botId;
      volatile int reserve;
      volatile boolean swapPending;
      volatile int cooldown;
      volatile int pearlCooldown;
      volatile int sinceDecision;
      volatile boolean errorLogged;
      volatile boolean armed;
      volatile boolean countingDown;
      volatile boolean control;
      volatile int strafeDir = 1;
      volatile int strafeLeft;
      volatile int pauseLeft;
      volatile int backTicks;
      volatile int attackCd;
      volatile int kbTicks;
      volatile boolean jumpNow;
      volatile int counterTicks;
      volatile int comboTicks;
      volatile int handSwapLeft;
      volatile int eatLeft;
      volatile int gappleCd;
      volatile int armorCheck;
      volatile double lead = (double)1.0F;
      volatile Hand hand;
      volatile Mode mode;
      volatile boolean advantage;
      volatile boolean pearlPending;
      Vector lastOwnerPos;
      Vector ownerVel;

      BotState(UUID owner) {
         this.hand = TKBott.Hand.SWORD;
         this.mode = TKBott.Mode.NEUTRAL;
         this.ownerVel = new Vector();
         this.owner = owner;
      }
   }

   private static record Saved(Block block, BlockData data) {
   }

   private static final class MenuHolder implements InventoryHolder {
      private Inventory inventory;

      public Inventory getInventory() {
         return this.inventory;
      }
   }
}
