package atpx.minecraft;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.block.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings({"UnusedDeclaration"})
public class IntoChest extends JavaPlugin implements Runnable, DataValues {

	private final Logger logger = Logger.getLogger("Minecraft");
	@SuppressWarnings({"FieldCanBeLocal"})
	private final long interval = 1000;

	private final ArrayList<Block> visitedB = new ArrayList<Block>();
	private final HashMap<String, ArrayList<String>> shortKeyValue = 
			new HashMap<String, ArrayList<String>>();

	private CommandSender debugClient;
	private boolean debugInfo, runtime;
	private PluginDescriptionFile pdf;
	private Block wildcardB;
	private Block fullB;

	public IntoChest() {
		for (final String key : DATAVALUES.keySet())
			ITEMNAMES.put(DATAVALUES.get(key), key);
	}

	public void onDisable() {
		logger.info(pdf.getName() + " is now disabled.");
	}

	public void onEnable() {
		pdf = getDescription();
		logger.info(pdf.getName() + " version " + pdf.getVersion() + 
		" is now enabled.");
		getServer().getScheduler().scheduleSyncRepeatingTask(this, this, 100L, 
		interval * 20L / 1000L);
	}

	@Override
	public boolean onCommand(final CommandSender sender, 
													 final Command command, 
													 final String label, 
													 final String[] args) {
		if (command.getName().equals("intochest")) {
			if (sender != null) {
				logger.info(pdf.getName() + ": '" + sender.getName() + "' called.");
				// If someone switches while it's already active
				if (debugClient != null && !debugClient.equals(sender))
					debugClient.sendMessage(ChatColor.RED + pdf.getName() + ": " + 
					ChatColor.GOLD + "'" + sender.getName() + "' called.");
				if (!"CONSOLE".equals(sender.getName()))
					debugClient = sender;
			}
			if (args.length == 2 &&
					(args[0].toLowerCase().equals("runtime") || 
					 args[0].toLowerCase().equals("debug")) &&
					(args[1].toLowerCase().equals("on") || 
					 args[1].toLowerCase().equals("off"))) {
				if (args[0].toLowerCase().equals("runtime")) {
					runtime = args[1].toLowerCase().equals("on");
					if (debugClient != null)
						debugClient.sendMessage(
								ChatColor.RED + pdf.getName() + ": " + ChatColor.GOLD + 
								"You " + (runtime ? "" : "de") + "activated runtime-monitoring."
						);
					if (sender != null)
						logger.info(pdf.getName() + ": '" + sender.getName() + "' " + 
					  (runtime ? "" : "de") + "activated runtime-monitoring.");
				} else {
					debugInfo = args[1].toLowerCase().equals("on");
					if (debugClient != null)
						debugClient.sendMessage(
								ChatColor.RED + pdf.getName() + ": " + ChatColor.GOLD + 
								"You " + (debugInfo ? "" : "de") + "activated debugging-infos."
						);
					if (sender != null)
						logger.info(pdf.getName() + ": '" + sender.getName() + "' " + 
					  (debugInfo ? "" : "de") + "activated debugging-infos.");
				}
				return true;
			}
		}
		return super.onCommand(sender, command, label, args);
	}

	public void debug(final String text) {
		if (debugInfo) {
			if (debugClient != null)
				debugClient.sendMessage(ChatColor.RED + pdf.getName() + ": " + 
			  ChatColor.GRAY + text);
			else
				logger.info(pdf.getName() + ": " + text);
		}
	}

	public void run() {
		final long start = System.nanoTime();

		try {
			for (final World world : getServer().getWorlds()) {
				for (final Entity entity : world.getEntities()) {
					if (entity instanceof Item) {
						final Item item = (Item) entity;
						Block workB = 
								item.getLocation().getBlock().getRelative(BlockFace.DOWN);
						if (workB.getType() == Material.WORKBENCH) {
							final ItemStack itemStack = item.getItemStack();
							final int itemTypeId = itemStack.getTypeId();
							final int itemDataVal = itemStack.getData().getData();
							debug("Processing Item: " + ITEMNAMES.get(itemTypeId + "|" + 
									itemDataVal) + "/" + itemTypeId + "/" + itemDataVal);
							debug("Item ID: " + itemStack.getTypeId());
							debug("Item Data Value: " + itemStack.getData().getData());
							debug("Item Name: " + ITEMNAMES.get(itemTypeId + "|" 
									+ itemDataVal));
							visitedB.clear(); // Flush list
							wildcardB = null;
							fullB = null;
						  // Check this and all linked other chests/dispensers
							workB = findNextEmptyContainer(workB, itemStack); 
							if (workB == null && fullB != null) {
								debug("Moving Item onto chest/dispenser due to full: X" + 
							  fullB.getX() + "/Y" + fullB.getY() + "/Z" + fullB.getZ());
								item.teleport(new Location(world, fullB.getX() + .5, fullB.getY() + 1, fullB.getZ() + .5));
							} else if (workB == null && wildcardB != null) {
						  	// Check all linked chests/dispensers from wildcard
								workB = findNextEmptyContainer(wildcardB, itemStack); 
							}
						  // Will return 'null' if no space left in chest/dispenser and 
							// linked chests/dispensers
							if (workB != null) {
								debug("Storing in chest/dispenser: X" + workB.getX() + 
										"/Y" + workB.getY() + "/Z" + workB.getZ());
								addContents(workB, itemStack);
								item.remove();
							}
							debug("");
						}
					}
				}
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, pdf.getName() + " threw " + e.getMessage(), e);
		}

		if (runtime) {
			final long end = System.nanoTime();
			final double duration = ((end - start) / 10000) / 100.0;
			if (debugClient != null)
				debugClient.sendMessage(ChatColor.RED + pdf.getName() + ": " + 
						ChatColor.GRAY + duration + "ms");
			logger.info(pdf.getName() + ": " + duration + "ms");
		}
	}

	public Block findNextEmptyContainer(final Block block, 
																			final ItemStack itemStack) {
		if (visitedB.contains(block))
			return null;

		if (block.getType() == Material.CHEST || 
				block.getType() == Material.DISPENSER) {
			if (isFull(getContents(block), itemStack)) {
				debug("Skipping chest/dispenser due to full: X" + block.getX() + 
						"/Y" + block.getY() + "/Z" + block.getZ());
				fullB = block;
			} else
				return block;
		}

		if (block.getType() == Material.SIGN_POST || 
				block.getType() == Material.WALL_SIGN) {
			if (isAllowed((Sign) block.getState(), itemStack.getTypeId(), 
					itemStack.getData().getData())) {
				debug("Alowed via sign found at: X" + block.getX() + "/Y" + 
					  block.getY() + "/Z" + block.getZ());
			} else {
				if (block.equals(wildcardB))
					debug("YET skipping because wildcard was found at: X" + 
				      block.getX() + "/Y" + block.getY() + "/Z" + block.getZ());
				else
					debug("Skipping because not allowed via sign found at: X" + 
				      block.getX() + "/Y" + block.getY() + "/Z" + block.getZ());
				return null;
			}
		}

		// Only process this block once per item or we will get infinite loops!
		visitedB.add(block); // Place this after the wildcard-return!

		final Block aboveBlock = block.getRelative(BlockFace.UP);
		final Material aboveType = aboveBlock.getType();

		Block newBlock = block.getRelative(BlockFace.NORTH);
		Material type = newBlock.getType();
		if (type == Material.CHEST || 
				type == Material.DISPENSER || 
				type == Material.REDSTONE_WIRE || 
				type == Material.SIGN_POST || 
				type == Material.WALL_SIGN) {
			final Block findBlock = findNextEmptyContainer(newBlock, itemStack);
			if (findBlock != null)
				return findBlock;
		} else if (type == Material.AIR) { // Down one step
			Block findBlock = newBlock.getRelative(BlockFace.DOWN);
			final Material findType = findBlock.getType();
			if (findType == Material.CHEST || 
					findType == Material.DISPENSER || 
					findType == Material.REDSTONE_WIRE || 
					findType == Material.SIGN_POST || 
					findType == Material.WALL_SIGN) {
				findBlock = findNextEmptyContainer(findBlock, itemStack);
				if (findBlock != null)
					return findBlock;
			}
		} else if (aboveType == Material.AIR) { // Up one step
			Block findBlock = aboveBlock.getRelative(BlockFace.NORTH);
			final Material findType = findBlock.getType();
			if (findType == Material.CHEST || 
					findType == Material.DISPENSER || 
					findType == Material.REDSTONE_WIRE || 
					findType == Material.SIGN_POST ||
					findType == Material.WALL_SIGN) {
				findBlock = findNextEmptyContainer(findBlock, itemStack);
				if (findBlock != null)
					return findBlock;
			}
		}

		newBlock = block.getRelative(BlockFace.EAST);
		type = newBlock.getType();
		if (type == Material.CHEST || 
				type == Material.DISPENSER || 
				type == Material.REDSTONE_WIRE || 
				type == Material.SIGN_POST || 
				type == Material.WALL_SIGN) {
			final Block findBlock = findNextEmptyContainer(newBlock, itemStack);
			if (findBlock != null)
				return findBlock;
		} else if (type == Material.AIR) { // Down one step
			Block findBlock = newBlock.getRelative(BlockFace.DOWN);
			final Material findType = findBlock.getType();
			if (findType == Material.CHEST || 
					findType == Material.DISPENSER || 
					findType == Material.REDSTONE_WIRE || 
					findType == Material.SIGN_POST || 
					findType == Material.WALL_SIGN) {
				findBlock = findNextEmptyContainer(findBlock, itemStack);
				if (findBlock != null)
					return findBlock;
			}
		} else if (aboveType == Material.AIR) { // Up one step
			Block findBlock = aboveBlock.getRelative(BlockFace.EAST);
			final Material findType = findBlock.getType();
			if (findType == Material.CHEST || 
					findType == Material.DISPENSER || 
					findType == Material.REDSTONE_WIRE || 
					findType == Material.SIGN_POST || 
					findType == Material.WALL_SIGN) {
				findBlock = findNextEmptyContainer(findBlock, itemStack);
				if (findBlock != null)
					return findBlock;
			}
		}

		newBlock = block.getRelative(BlockFace.SOUTH);
		type = newBlock.getType();
		if (type == Material.CHEST || type == Material.DISPENSER || 
				type == Material.REDSTONE_WIRE || type == Material.SIGN_POST
				|| type == Material.WALL_SIGN) {
			final Block findBlock = findNextEmptyContainer(newBlock, itemStack);
			if (findBlock != null)
				return findBlock;
		} else if (type == Material.AIR) { // Down one step
			Block findBlock = newBlock.getRelative(BlockFace.DOWN);
			final Material findType = findBlock.getType();
			if (findType == Material.CHEST || findType == Material.DISPENSER || 
					findType == Material.REDSTONE_WIRE || findType == Material.SIGN_POST
					|| findType == Material.WALL_SIGN) {
				findBlock = findNextEmptyContainer(findBlock, itemStack);
				if (findBlock != null)
					return findBlock;
			}
		} else if (aboveType == Material.AIR) { // Up one step
			Block findBlock = aboveBlock.getRelative(BlockFace.SOUTH);
			final Material findType = findBlock.getType();
			if (findType == Material.CHEST || findType == Material.DISPENSER || 
					findType == Material.REDSTONE_WIRE || findType == Material.SIGN_POST
					|| findType == Material.WALL_SIGN) {
				findBlock = findNextEmptyContainer(findBlock, itemStack);
				if (findBlock != null)
					return findBlock;
			}
		}

		newBlock = block.getRelative(BlockFace.WEST);
		type = newBlock.getType();
		if (type == Material.CHEST || type == Material.DISPENSER || 
				type == Material.REDSTONE_WIRE || type == Material.SIGN_POST
				|| type == Material.WALL_SIGN) {
			final Block findBlock = findNextEmptyContainer(newBlock, itemStack);
			if (findBlock != null)
				return findBlock;
		} else if (type == Material.AIR) { // Down one step
			Block findBlock = newBlock.getRelative(BlockFace.DOWN);
			final Material findType = findBlock.getType();
			if (findType == Material.CHEST || findType == Material.DISPENSER || 
					findType == Material.REDSTONE_WIRE || findType == Material.SIGN_POST
					|| findType == Material.WALL_SIGN) {
				findBlock = findNextEmptyContainer(findBlock, itemStack);
				if (findBlock != null)
					return findBlock;
			}
		} else if (aboveType == Material.AIR) { // Up one step
			Block findBlock = aboveBlock.getRelative(BlockFace.WEST);
			final Material findType = findBlock.getType();
			if (findType == Material.CHEST || findType == Material.DISPENSER || 
					findType == Material.REDSTONE_WIRE || findType == Material.SIGN_POST
					|| findType == Material.WALL_SIGN) {
				findBlock = findNextEmptyContainer(findBlock, itemStack);
				if (findBlock != null)
					return findBlock;
			}
		}

		return null;
	}

	private boolean isAllowed(final Sign sign, 
			                      final int typeId, 
			                      final int dataVal) {
		boolean retVal = true, fit = false;
		for (String key : (sign.getLine(0) + " " + sign.getLine(1) + " " + 
		                   sign.getLine(2) + " " + 
				               sign.getLine(3)).toLowerCase().split(",")) {
			key = key.trim(); // Strip leading and trailing spaces...

			String value = "";
			if (key.equals(ITEMNAMES.get(typeId + "|" + dataVal))) {
				debug("Fit via exact name: " + key);
				value = key;
			} else if (isValueForShortKey(key, typeId)) {
				debug("Fit via short name: " + key + " -> " + ITEMNAMES.get(typeId));
				value = key;
			}
			if (value == null && key.equals(typeId + "|" + dataVal)) { // Block/Item decimal number
				debug("Fit via id: " + key);
				value = key;
			}

			
			if (value != null) {
				retVal &= (value == (typeId+"|"+dataVal));
				fit = true;
			}
	

			if (key.matches("^\\*$")) { // Wildcard sign, means "if nothing else matches"
				debug("We have a wildcard! X" + sign.getBlock().getX() + "/Y" + 
			  sign.getBlock().getY() + "/Z" + sign.getBlock().getZ());
				if (wildcardB == null) {
					// On the first run stop processing the wildcard and do the rest first.
					wildcardB = sign.getBlock();
					return false;
				} else {
					// Then, on the second run, after everything else, do the wildcard.
					wildcardB = null;
					return true;
				}
			}
		}
		return fit && retVal;
	}

	private boolean isValueForShortKey(final String key, final int typeId) {
		return getKeyValueForThisShortKey(key).contains(typeId);
	}

	private Integer getValueForShortKey(final String key, final int typeId) {
		return getKeyValueForThisShortKey(key).contains(typeId) ? typeId : null;
	}

	private ArrayList<String> getKeyValueForThisShortKey(final String key) {
		if (!shortKeyValue.containsKey(key)) { // Compile the typeID-list only once per shortKey
			debug("Starting new list: " + key);
			final ArrayList<String> list = new ArrayList<String>();
			for (final String dataValueKey : DATAVALUES.keySet()) {
				if (dataValueKey.indexOf(key) != -1) {
					debug("Adding to list '" + key + "': " + DATAVALUES.get(dataValueKey));
					list.add(DATAVALUES.get(String.valueOf(dataValueKey)));
				}
			}
			shortKeyValue.put(key, list);
		}
		return shortKeyValue.get(key);
	}

	public boolean isFull(final ItemStack[] items, final ItemStack check) {
		int amt = check.getAmount();
		debug("To stack: " + amt);
		for (final ItemStack item : items) {
			if (item == null || item.getAmount() == 0)
				return false; // Empty slot -> Chest/Dispenser is not full for sure. :)
			if (item.getTypeId() == check.getTypeId()) { // Contains at least one item of same type
				if (TYPEDEPENDANTSTACKABLE.contains(item.getTypeId())) {
					if (item.getData().getData() == check.getData().getData())
						amt -= 64 - item.getAmount();
					if (amt <= 0)
						debug("stackable(64,type): " + amt);
				} else if (!NOTSTACKABLE.contains(item.getTypeId())) { // This one needs to be the last one
					amt -= 64 - item.getAmount();
					if (amt <= 0)
						debug("stackable(64): " + amt);
				} else
					debug("NOT stackable!");

				if (amt <= 0)
					return false;
			}
		}
		return true;
	}

	public ItemStack[] getContents(final Block block) {
		final BlockState blockState = block.getState();
		if (blockState instanceof Chest) {
			final Chest chest = (Chest) blockState;
			final Chest otherChest = otherChest(block);
			if (otherChest == null)
				return chest.getInventory().getContents();
			else {
				final ArrayList<ItemStack> t = new ArrayList<ItemStack>();
				if (otherChest.getX() < block.getX() || 
						otherChest.getZ() < block.getZ()) {
					t.addAll(Arrays.asList(otherChest.getInventory().getContents()));
					t.addAll(Arrays.asList(chest.getInventory().getContents()));
				} else {
					t.addAll(Arrays.asList(chest.getInventory().getContents()));
					t.addAll(Arrays.asList(otherChest.getInventory().getContents()));
				}
				return t.toArray(new ItemStack[t.size()]);
			}
		} else if (blockState instanceof Dispenser) {
			return ((Dispenser) blockState).getInventory().getContents();
		} else {
			logger.warning("Unknown BlockState: " + blockState);
			return null;
		}
	}

	public void addContents(final Block block, final ItemStack stack) {
		final BlockState blockState = block.getState();
		if (blockState instanceof Chest) {
			final Chest chest = (Chest) blockState;
			final Chest otherChest = otherChest(block);
			if (otherChest == null)
				chest.getInventory().addItem(stack);
			else if ((otherChest.getX() < block.getX()) || 
					     (otherChest.getZ() < block.getZ())) {
				if (isFull(otherChest.getInventory().getContents(), stack))
					chest.getInventory().addItem(stack);
				else
					otherChest.getInventory().addItem(stack);
			} else {
				if (isFull(chest.getInventory().getContents(), stack))
					otherChest.getInventory().addItem(stack);
				else
					chest.getInventory().addItem(stack);
			}
		} else if (blockState instanceof Dispenser) {
			((Dispenser) blockState).getInventory().addItem(stack);
		} else {
			logger.warning("Unknown BlockState: " + blockState);
		}
	}

	private static Chest otherChest(final Block block) {
		if (block == null)
			return null;
		if (block.getRelative(BlockFace.NORTH).getType() == Material.CHEST)
			return (Chest) block.getRelative(BlockFace.NORTH).getState();
		if (block.getRelative(BlockFace.WEST).getType() == Material.CHEST)
			return (Chest) block.getRelative(BlockFace.WEST).getState();
		if (block.getRelative(BlockFace.SOUTH).getType() == Material.CHEST)
			return (Chest) block.getRelative(BlockFace.SOUTH).getState();
		if (block.getRelative(BlockFace.EAST).getType() == Material.CHEST)
			return (Chest) block.getRelative(BlockFace.EAST).getState();
		return null;
	}
}
