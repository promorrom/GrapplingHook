package us.mytheria.gh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class GrapplingHook extends JavaPlugin implements Listener, CommandExecutor {

	ItemStack gH;
	NamespacedKey hK = new NamespacedKey(this, "hookKey");

	public HashMap<Integer, Integer> noFallEntities = new HashMap<Integer, Integer>();

	String dN = "ItemMeta.DisplayName";
	String mL = "ItemMeta.Lore";
	String mD = "ItemMeta.CustomModelData";
	String coolDown = "Mechanics.Cooldown";
	String badRodBoolean = "Mechanics.UpdateItemMeta";
	String statTrack = "Mechanics.StatTrack";
	String fallDamage = "Mechanics.FallDamage";
	String getPerm = "Permissions.Administrator";
	String noPerm = "Messages.NoPermission";
	String badRod = "Messages.WrongModelData";
	String cM = "Messages.Cooldown";

	/* Defaults */
	String[] grapplingLore = { ChatColor.GRAY + "Travel around in style using", ChatColor.GRAY + "this Grappling Hook",
			ChatColor.DARK_GRAY + "2 Second Cooldown", "",
			ChatColor.GRAY + "Total uses: " + ChatColor.WHITE + "%totalUses%", "",
			ChatColor.GREEN.toString() + ChatColor.BOLD + "UNCOMMON" };
	HashMap<String, Long> cooldown = new HashMap<>();

	public void loadConfiguration() {
		getConfig().addDefault(dN, ChatColor.GREEN + "Grappling Hook");
		getConfig().addDefault(mL, Arrays.asList(grapplingLore));
		getConfig().addDefault(mD, 12312020);
		getConfig().addDefault(coolDown, 2);
		getConfig().addDefault(badRodBoolean, true);
		getConfig().addDefault(statTrack, true);
		getConfig().addDefault(fallDamage, true);
		getConfig().addDefault(cM, ChatColor.RED + "Whow! Slow down there!");
		getConfig().addDefault(noPerm, "Not enough permissions");
		getConfig().addDefault(badRod, "Outdated item. Grappling Hook updated");
		getConfig().addDefault(getPerm, "mytheriautils.grapplinghook");
		getConfig().addDefault(getPerm, "mytheriautils.reload");
		getConfig().options().copyDefaults(true);
		saveConfig();
	}

	public void applyItemMeta(ItemStack is) {
		ItemMeta im = is.getItemMeta();
		im.getPersistentDataContainer().set(hK, PersistentDataType.INTEGER, 0);
		im.setDisplayName(getConfig().getString(dN));
		im.setLore(getConfig().getStringList(mL));
		int stat = im.getPersistentDataContainer().get(hK, PersistentDataType.INTEGER);
		List<String> desc = im.getLore();
		desc = desc.stream().map(s -> s.replace("%totalUses%", String.valueOf(stat))).collect(Collectors.toList());
		im.setLore(desc);
		im.setCustomModelData(getConfig().getInt(mD));
		im.setUnbreakable(true);
		im.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
		is.setItemMeta(im);
	}

	public void countStat(ItemStack i, ItemMeta im) {
		int stat = im.getPersistentDataContainer().get(hK, PersistentDataType.INTEGER) + 1;
		im.getPersistentDataContainer().set(hK, PersistentDataType.INTEGER, stat);
		im.setLore(getConfig().getStringList(mL));
		List<String> desc = im.getLore();
		desc = desc.stream().map(s -> s.replace("%totalUses%", String.valueOf(stat))).collect(Collectors.toList());
		im.setLore(desc);
		i.setItemMeta(im);
	}

	public void doGrapple(Player p, PlayerFishEvent e, ItemStack i, ItemMeta im) {
		if (!p.hasPermission(getConfig().getString(getPerm))) {
			Location loc = e.getHook().getLocation();
			if (cooldown.containsKey(p.getName())) {
				if ((cooldown.get(p.getName()) + getConfig().getLong(coolDown)) < (System.currentTimeMillis() / 1000)) {
					cooldown.put(e.getPlayer().getName(), (System.currentTimeMillis() / 1000));
					pullEntityToLocation(p, loc);
					if (getConfig().getBoolean(statTrack) == true) {
						countStat(i, im);
					}
				} else {
					p.sendMessage(getConfig().getString(cM));
				}
			} else {
				cooldown.put(e.getPlayer().getName(), (System.currentTimeMillis() / 1000));
				pullEntityToLocation(p, loc);
				if (getConfig().getBoolean(statTrack) == true) {
					countStat(i, im);
				}
			}
		} else {
			Location loc = e.getHook().getLocation();
			pullEntityToLocation(p, loc);
			if (getConfig().getBoolean(statTrack) == true) {
				countStat(i, im);
			}
		}
	}

	public void onEnable() {
		loadConfiguration();
		Bukkit.getPluginManager().registerEvents(this, this);

		gH = new ItemStack(Material.FISHING_ROD);
		applyItemMeta(gH);

	}

	public void renameLater(PlayerFishEvent e, ItemStack i) {
		Player p = e.getPlayer();
		if (i != null && i.hasItemMeta()) {
			ItemMeta im = i.getItemMeta();
			if (im.getPersistentDataContainer().has(hK, PersistentDataType.INTEGER)) {
				if (im.getCustomModelData() == getConfig().getInt(mD)) {
					if (e.getState() == State.FISHING) {
						FishHook h = e.getHook();
						h.setMaxWaitTime(20 * 5);
						h.setMinWaitTime(20 * 2);
						h.setVelocity(new Vector(h.getVelocity().getX(), h.getVelocity().getY() * 0.80,
								h.getVelocity().getZ()));
					}
					if (e.getState() == State.REEL_IN || e.getState() == State.IN_GROUND) {
						doGrapple(p, e, i, im);

					}
					if (e.getState() == State.CAUGHT_FISH) {
						ItemStack fished = ((Item) e.getCaught()).getItemStack();
						fished.setAmount(0);
						e.setExpToDrop(0);
						doGrapple(p, e, i, im);
					}
				} else {
					if (getConfig().getBoolean(badRodBoolean) == true) {
						ItemStack h = i;
						im.setCustomModelData(getConfig().getInt(mD));
						h.setItemMeta(im);
						p.sendMessage(getConfig().getString(badRod));
					}
				}
			}
		}

	}

	public void doAnvil(PrepareAnvilEvent e, ItemStack i) {
		if (i != null && i.hasItemMeta()) {
			ItemMeta im = i.getItemMeta();
			if (im.getPersistentDataContainer().has(hK, PersistentDataType.INTEGER)) {
				if (im.getCustomModelData() != getConfig().getInt(mD)) {
					if (getConfig().getBoolean(badRodBoolean) == true) {
						int stat = im.getPersistentDataContainer().get(hK, PersistentDataType.INTEGER);
						im.setDisplayName(getConfig().getString(dN));
						im.setCustomModelData(getConfig().getInt(mD));
						im.setLore(getConfig().getStringList(mL));
						List<String> desc = im.getLore();
						desc = desc.stream().map(s -> s.replace("%totalUses%", String.valueOf(stat)))
								.collect(Collectors.toList());
						im.setLore(desc);
						i.setItemMeta(im);
						if (e.getViewers().size() > 0) {
							Player p = (Player) e.getViewers().get(0);
							p.sendMessage(getConfig().getString(badRod));

						}
					}
				}
				e.setResult(null);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onAnvil(PrepareAnvilEvent e) {
		if (e.getInventory() != null) {
			doAnvil(e, e.getInventory().getItem(0));
			doAnvil(e, e.getInventory().getItem(1));
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEnchant(EnchantItemEvent e) {
		ItemStack is = e.getItem();
		ItemMeta im = is.getItemMeta();
		if (im.getPersistentDataContainer().has(hK, PersistentDataType.INTEGER)) {
			if (im.getCustomModelData() != getConfig().getInt(mD)) {
				if (getConfig().getBoolean(badRodBoolean) == true) {
					int stat = im.getPersistentDataContainer().get(hK, PersistentDataType.INTEGER);
					im.setDisplayName(getConfig().getString(dN));
					im.setCustomModelData(getConfig().getInt(mD));
					im.setLore(getConfig().getStringList(mL));
					List<String> desc = im.getLore();
					desc = desc.stream().map(s -> s.replace("%totalUses%", String.valueOf(stat)))
							.collect(Collectors.toList());
					im.setLore(desc);
					is.setItemMeta(im);
					if (e.getViewers().size() > 0) {
						Player p = (Player) e.getViewers().get(0);
						p.sendMessage(getConfig().getString(badRod));

					}
				}
			}
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onRodLand(PlayerFishEvent e) {
		renameLater(e, e.getPlayer().getInventory().getItemInMainHand());
		renameLater(e, e.getPlayer().getInventory().getItemInOffHand());
	}

	private void pullEntityToLocation(final Entity e, Location loc) {
		Location entityLoc = e.getLocation();

		double g = -0.08;
		double d = loc.distance(entityLoc);
		double t = d;
		double v_x = (1.0 + 0.07 * t) * (loc.getX() - entityLoc.getX()) / t;
		double v_y = (1.0 + 0.03 * t) * (loc.getY() - entityLoc.getY()) / t - 0.5 * g * t;
		double v_z = (1.0 + 0.07 * t) * (loc.getZ() - entityLoc.getZ()) / t;

		Vector v = e.getVelocity();
		v.setX(v_x * 3.75);
		v.setY(v_y);
		v.setZ(v_z * 3.75);
		e.setVelocity(v);

		addNoFall(e, 100);
	}

	public void addNoFall(final Entity e, int ticks) {
		if (noFallEntities.containsKey(e.getEntityId()))
			Bukkit.getServer().getScheduler().cancelTask(noFallEntities.get(e.getEntityId()));

		int taskId = this.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			@Override
			public void run() {
				if (noFallEntities.containsKey(e.getEntityId()))
					noFallEntities.remove(e.getEntityId());
			}
		}, ticks);

		noFallEntities.put(e.getEntityId(), taskId);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onFallDamage(EntityDamageEvent e) {
		if (e.getCause() == DamageCause.FALL && getConfig().getBoolean(fallDamage) == true) {
			if (noFallEntities.containsKey(e.getEntity().getEntityId())) {
				e.setCancelled(true);
			}
		}
	}

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equalsIgnoreCase("grapplinghook")) {
			CommandSender s = sender;
			if (sender.hasPermission(getConfig().getString(getPerm))) {
				if (args.length == 0) {
					s.sendMessage("No arguments provided. Use: /gh <reload/get>");
				} else {
					switch (args[0].toLowerCase()) {
					case "reload":
						reloadConfig();
						applyItemMeta(gH);
						s.sendMessage("Config reloaded");
						break;
					case "get":
						if (s instanceof Player) {
							Player p = (Player) s;
							p.getInventory().addItem(gH);
						}
						break;
					case "list":
						for (String i : cooldown.keySet()) {
							sender.sendMessage(i);
						}
						break;
					default:
						s.sendMessage("Invalid argument. Use /gh <reload/get>");
					}

				}
			} else {
				sender.sendMessage(getConfig().getString(noPerm));
			}
		}
		return false;
	}

	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (command.getName().equalsIgnoreCase("grapplinghook")) {
			if (sender.hasPermission(getConfig().getString(getPerm))) {
				List<String> l = new ArrayList<String>();
				if (args.length == 1) {
					l.add("get");
					l.add("reload");
				}
				return l;
			}
		}
		return null;
	}

}
