/*
 *  IntoChest - A Minecraft Spigot Plugin
 *  Copyright (C) 2015-2016  Jeff Lee
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program, under the name of "license.txt"; if not, write to the 
 *  Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, 
 *  MA 02110-1301 USA.
 */
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
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@code IntoChest} is the driver for the IntoChest Minecraft plugin.
 * <p>
 * Runs continuously while plugin is enabled, checking for any items that
 * lie on top of a crafting table. When found, it will process
 * the item and attempt to find a chest to place the item into. The chest
 * may be next to the crafting table or be connected through redstone wire
 * and signs. 
 * <p>
 * Signs can be used to filter items from its redstone path. 
 * You can filter an item by typing one of these identifiers on the sign:
 * <br>
 * <ol>
 * <li>The full name of the item
 * <li>The short name of the item
 * <li>The group name that includes the item
 * <li>The decimal item ID of the item
 * <li>The decimal item ID and item data value of the item, delimited by a pipe
 * </ol>
 * <p>
 * <b>Example</b>: I want to only allow birch wood to pass through a sign.
 * I can type one these onto the sign to get the desired results:
 * <br>
 * <ol>
 * <li>birch wood
 * <li>bwood
 * <li>wood
 * <li>17
 * <li>17|2
 * </ol>
 * <p>
 * For how to derive the short name of an item, refer to {@link DataValues
 * SHORTITEMNAMES}.
 * <p>
 * You may specify multiple items on a sign - you must separate items with 
 * the comma (,) delimiter.
 * <p>
 * @author Jeff Lee
 */
public class IntoChest 
		extends JavaPlugin 
		implements Runnable, DataValues, Listener {

	/* ****CONSTANTS**** */
	private final long INTERVAL = 1000;    //Time interval for scheduler	
	private final String DELIMITER = "|";  //Delimiter for item IDs and item data value
	
	/* ****HIGH-LEVEL VARIABLES**** */
	private Logger logger = Logger.getLogger("Minecraft"); //Used for sending messages to the server console
	private CommandSender clientConsole;									 //Used for sending messages to the user client
	private PluginDescriptionFile pdf;                     //Plugin metadata
	
  /* ****VARIABLES**** */
	//Holds list of blocks that have been visited during traversal, prevent infinite loops
	private ArrayList<Block> visitedB = new ArrayList<Block>(); 
	//Debug message toggle flags
	private boolean isGeneralDebugOn;
	private boolean isFilterDebugOn;
	private boolean isRuntimeDebugOn;
	private boolean isPathingDebugOn;
	
	private Block wildcardB;               //Holds wildcard block
	private Block fullB;                   //Holds block that is found to be full
	private String pluginName = "";
	
  //Map users who are about to check the info for an item they're holding or
  //the sign they are about it interact with
	private HashMap<String, String> playersChecking =   
			new HashMap<String, String>();

	/**
	 * Default constructor.
	 */
	public IntoChest() {
	}

	/**
	 * Called when plugin is disabled.
	 */
	public void onDisable() {
		logger.info(pluginName + " is now disabled.");
		
		HandlerList.unregisterAll((Plugin) this);
	}

	/**
	 * Called when plugin is enabled.
	 */
	public void onEnable() {
		pdf = getDescription();
		pluginName = pdf.getName();
		logger.info(pluginName + " version " + pdf.getVersion() + 
				" is now enabled.");
		
		getServer().getPluginManager().registerEvents(this, this);
		
		//This plugin loops in order to constantly check for items sitting on
		//crafting tables. 
		getServer().getScheduler().scheduleSyncRepeatingTask(this, this, 100L, 
		INTERVAL * 20L / 1000L);
	}

	/**
	 * Handles when user enters a command for this plugin. 
	 * <p>
	 * Available commands:
	 * <br>
	 * <ul>
	 * <li>help === Show help text
	 * <li>debug - on/off - general/runtime/filter/pathing === Types of debug messages
	 * </ul>
	 * <p>
	 * @param sender  user who sent the command
	 * @param command  the command sent
	 * @param args  command arguments
	 * @return true if command was successfully handled
	 */
	@Override
	public boolean onCommand(CommandSender sender, 
													 Command command, 
													 String label, 
													 String[] args) {
		String senderName = "";
		boolean doEnable = false;
		int argsLength; 
		
		//Incoming command is not valid for plugin
		if (!command.getName().equalsIgnoreCase("intochest")) {
			return false;
		}
			
		//Notify server console that user called a command
		if (sender != null) {
			senderName = sender.getName();
			logger.info(pluginName + ": '" + senderName + "' called.");
			// If someone switches while it's already active
			if (clientConsole != null && !clientConsole.equals(sender)) {
				clientConsole.sendMessage(ChatColor.RED + pluginName + ": " + 
				ChatColor.GOLD + "'" + senderName + "' called.");
			}
			if (!"CONSOLE".equals(senderName)) {
				clientConsole = sender;
			}
		}
	
		//Having no arguments given is bad
		argsLength = args.length;
		if (argsLength == 0) {
			return false;
		}
		
		//HELP
		if (args[0].equalsIgnoreCase("help")) {
			StringBuilder str = new StringBuilder();
			
			str.append("\n");
			str.append(ChatColor.WHITE + "==========");
			str.append(ChatColor.BLUE + "IntoChest");
			str.append(ChatColor.WHITE + "========== \n");
			//str.append(ChatColor.DARK_AQUA + "Full reference available at: ");
			//str.append(ChatColor.AQUA + "www.placeholder.com \n");
			str.append(ChatColor.LIGHT_PURPLE + "/ic debug ");
			str.append(ChatColor.RED + "on");
			str.append(ChatColor.WHITE + "|");
			str.append(ChatColor.RED + "off ");
			str.append(ChatColor.GOLD + "general");
			str.append(ChatColor.WHITE + "|");
			str.append(ChatColor.GOLD + "filter");
		  str.append(ChatColor.WHITE + "|");
		  str.append(ChatColor.GOLD + "pathing");
		  str.append(ChatColor.WHITE + "|");
		  str.append(ChatColor.GOLD + "runtime ");
			str.append(ChatColor.GRAY + "=== " +
					"Toggle general, sign filtering, item pathing, or runtime messages. \n");
			str.append(ChatColor.LIGHT_PURPLE + "/ic help ");
			str.append(ChatColor.GRAY + "=== Get some help text. \n");
	    str.append(ChatColor.LIGHT_PURPLE + "/ic inspect ");
	    str.append(ChatColor.GRAY + "=== Get the sign filter identifiers for an " +
	    		"item in your hand or allowed/invalid items on a sign. \n");
	    str.append(ChatColor.LIGHT_PURPLE + "/ic inspect "); 
	    str.append(ChatColor.RED + "on");
	    str.append(ChatColor.WHITE + "|");
	    str.append(ChatColor.RED + "off ");
	    str.append(ChatColor.GRAY + "=== Persistently keep item inspection " + 
	    		"on or off. \n");
			str.append(ChatColor.WHITE + "==============================");
	    
	    sendMsgToClient(MessageType.ONCOMMAND, str.toString());
			
			return true;
		}
		//DEBUG
		else if (args[0].equalsIgnoreCase("debug")) {
			
			//No arguments for on/off of DEBUG, so toggle all message types
			if (argsLength == 2) {
				if (args[1].equalsIgnoreCase("on")) {
					doEnable = true;
				}
				else if (args[1].equalsIgnoreCase("off")) {
					doEnable = false;
				}
				else {
					return false;
				}
				
				if (doEnable) {
					isGeneralDebugOn = true;
					isRuntimeDebugOn = true;
					isFilterDebugOn = true;
					isPathingDebugOn = true;
				}
				else {
					isGeneralDebugOn = false;
					isRuntimeDebugOn = false;
					isFilterDebugOn = false;
					isPathingDebugOn = false;
				}
			
				sendMsgToClient(MessageType.ONCOMMAND,
					                "You " + (doEnable ? "" : "de") + 
					                "activated all debug messages.");
			  if (sender != null) {
			  	sendMsgToLogger(MessageType.ONCOMMAND,
			  										senderName + "' " + 
					                  (doEnable ? "" : "de") + 
					                  "activated all debug messages.");
			  }
			  
			  return true;
		  }
			//Argument given for DEBUG on/off, so discretely toggle the specific message
			//type given by user. 
			else if (argsLength == 3) {
				//Split up argument holding string of message types delimited by comma and
				//process each message type.
				for(String tokenStr : args[2].split(",")) {
					tokenStr = tokenStr.trim();
					
					//GENERAL MESSAGE TYPE
					if (tokenStr.equalsIgnoreCase("general")) { 
						if (doEnable) {
							isGeneralDebugOn = true;
						}
						else {
							isGeneralDebugOn = false;
						}
						sendMsgToClient(MessageType.ONCOMMAND,
                "You " + (isGeneralDebugOn ? "" : "de") + 
                "activated general debugging messages.");
						if (sender != null) {
							sendMsgToLogger(MessageType.ONCOMMAND,
									senderName + "' " + 
                  (isGeneralDebugOn ? "" : "de") + 
                  "activated general debugging messages.");
						}
					}
					//RUNTIME MESSAGE TYPE
					else if (tokenStr.equalsIgnoreCase("runtime")) {
						if (doEnable) {
							isRuntimeDebugOn = true;
						}
						else {
							isRuntimeDebugOn = false;
						}
						sendMsgToClient(MessageType.ONCOMMAND,
                "You " + (isRuntimeDebugOn ? "" : "de") + 
                "activated runtime monitoring.");
						if (sender != null) {
							sendMsgToLogger(MessageType.ONCOMMAND,
									senderName + "' " + 
                  (isRuntimeDebugOn ? "" : "de") + 
                  "activated runtime monitoring.");
						}
					}
					//FILTER MESSAGE TYPE
					else if (tokenStr.equalsIgnoreCase("filter")) {
						if (doEnable) {
							isFilterDebugOn = true;
						}
						else {
							isFilterDebugOn = false;
						}
						sendMsgToClient(MessageType.ONCOMMAND,
                "You " + (isFilterDebugOn ? "" : "de") + 
                "activated filter debug messages.");
						if (sender != null) {
							sendMsgToLogger(MessageType.ONCOMMAND,
									senderName + "' " + 
                  (isFilterDebugOn ? "" : "de") + 
                  "activated filter debug messages.");
            }
					}
					//PATHING MESSAGE TYPE
					else if (tokenStr.equalsIgnoreCase("pathing")) {
						if (doEnable) {
							isPathingDebugOn = true;
						}
						else {
							isPathingDebugOn = false;
						}
						sendMsgToClient(MessageType.ONCOMMAND,
                "You " + (isPathingDebugOn ? "" : "de") + 
                "activated pathing debug messages.");
						if (sender != null) {
							sendMsgToLogger(MessageType.ONCOMMAND,
									senderName + "' " + 
                  (isPathingDebugOn ? "" : "de") + 
                  "activated pathing debug messages.");
            }
					}
				}
				//Attempted to toggle debug message types. Consider successful regardless if valid.
				return true;
			}
			//Having an invalid number of arguments for DEBUG is bad
			else {
				return false;
			}
		}
		//Inspect an item.
		else if (args[0].equalsIgnoreCase("inspect")) {
			if (sender == null) {
				return false;
			}
			
			//If no arguments given for inspecting, just allow one item to be inspected
			if (args.length == 1) {
				if (!playersChecking.containsKey(senderName)) {
					playersChecking.put(senderName, "once");
				}
				sendMsgToClient(MessageType.ONCOMMAND, 
						"Inspect an item you are holding by clicking with the item in hand " +
				    "or a sign by clicking on the sign.");	
				return true;
			}
			//If an argument of "on" is given, set persistent checking for player.
			//If "off" is given, turn off inspecting for player.
			else if (args.length == 2) {
				if (args[1].equalsIgnoreCase("on")) {
					playersChecking.put(senderName, "persistent");
					sendMsgToClient(MessageType.ONCOMMAND, 
							"Inspecting mode has been turned ON. \n Inspect an item you are holding " +
					    "by clicking with the item in hand " +
					    "or a sign by clicking on the sign.");	
					return true;
				}
				else if (args[1].equalsIgnoreCase("off")) {
					playersChecking.remove(senderName);
					sendMsgToClient(MessageType.ONCOMMAND, 
							"Inspecting mode has been turned OFF.");	
					return true;
				}
				else {
					return false;
				}
			}
			else {
				return false;
			}
		}
		//Having an invalid first argument is bad
		else {
			return false;
		}
	}
  
	/**
	 * Checks for any items that are sitting on a crafting table and processes
	 * those items to be placed in a chest or dispenser (end component), 
	 * along with accounting for sign filters.
	 */
	public void run() {
	  long start = System.nanoTime();
	  long end;
	  double duration;
	  
	  Item item;
	  Block workB;
	  ItemStack itemStack;  //Item stack data
	  int itemTypeId;       //Item decimal ID
	  int itemDataVal;      //Item data value
	  String itemIdStr;
	  
	  //Loop through every entity in every world of the server and check if
	  //the entity is an item sitting on top of a crafting table.
		try {
			for (World world : getServer().getWorlds()) {
				for (Entity entity : world.getEntities()) {
					if (entity instanceof Item) {
						item = (Item) entity;
						workB = 
								item.getLocation().getBlock().getRelative(BlockFace.DOWN);
						if (workB.getType() == Material.WORKBENCH) {
						  itemStack = item.getItemStack();                
							itemTypeId = itemStack.getTypeId();								
							itemDataVal = itemStack.getData().getData();         
							itemIdStr = itemTypeId + DELIMITER + itemDataVal; 
							
							sendMsgToClient(MessageType.GENERAL, 
									"\n\nProcessing item: " + ITEMNAMES.get(itemIdStr) + 
									" / " + itemTypeId + " / " + itemDataVal);
							
							visitedB.clear(); //Flush list of traversed blocks
							wildcardB = null; //??
							fullB = null;     //Holds an end component that is full
							
							//Look for a valid chest/dispenser, traversing through redstone path
							workB = findNextEmptyComponent(workB, itemStack, itemIdStr);
							
							//A full end component was found but there is no non-full end
							//component found, so send the item to sit ON TOP of the full end component.
							if (workB == null && fullB != null) {
								sendMsgToClient(MessageType.PATHING,
										"Moving item on top of chest/dispenser because it's full: X" + 
										fullB.getX() + "/Y" + fullB.getY() + "/Z" + fullB.getZ());
								item.teleport(new Location(world, fullB.getX() + .5, 
										fullB.getY() + 1, fullB.getZ() + .5));
							} 
							//A wildcard end block was found but there is no non-full end
							//component found, so traverse for a valid end component from the
							//wildcard end block.
							else if (workB == null && wildcardB != null) {
								workB = findNextEmptyComponent(wildcardB, itemStack, itemIdStr); 
							}
							
							//A valid end component was found so store the item into this
							//end component.
							if (workB != null) {
								sendMsgToClient(MessageType.GENERAL,
										"Storing in chest/dispenser: X" + workB.getX() + 
										"/Y" + workB.getY() + "/Z" + workB.getZ());
								addContents(workB, itemStack);
								item.remove();
							}
							else {
								sendMsgToClient(MessageType.GENERAL,
										"No chest/dispenser to send item!");
							}
						}
					}
				}
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, pluginName + " threw " + e.getMessage(), e);
		}

		//Report runtime monitoring
		if (isRuntimeDebugOn) {
		  end = System.nanoTime();
			duration = ((end - start) / 10000) / 100.0;
			sendMsgToClient(MessageType.RUNTIME, duration + "ms");
			sendMsgToLogger(MessageType.RUNTIME, duration + "ms");
		}
	}
	
	/**
	 * Recursively traverses the qualifying path between crafting table and
	 * end component while checking for filters. 
	 * 
	 * @param curBlock  current block being pointed at
	 * @param itemStack  item that is sitting on the crafting table
	 * @param itemIdStr  item identifier of item that is sitting on the crafting table
	 * @return a block that is found to be the end component or null if no
	 * 				 block was found
	 */
	protected Block findNextEmptyComponent(Block curBlock, 
																			ItemStack itemStack,
																			String itemIdStr) {
		if (visitedB.contains(curBlock))
			return null;

		if (isBlockAnEndComponent(curBlock)) {
			if (isEndComponentFull(getContents(curBlock), itemStack)) {
				sendMsgToClient(MessageType.PATHING, 
						"Skipping chest/dispenser because it's full: X" + 
						curBlock.getX() + "/Y" + curBlock.getY() + "/Z" + curBlock.getZ());
				fullB = curBlock;
			} 
			else
			{
				return curBlock;
			}
		}

		if (isBlockAFilterComponent(curBlock)) {
			Sign sign = (Sign) curBlock.getState();
			if (isAllowed(sign, itemStack.getTypeId(), 
					itemStack.getData().getData(), itemIdStr)) {
				sendMsgToClient(MessageType.PATHING,
						"Allowed via sign found at: X" + curBlock.getX() + "/Y" + 
						curBlock.getY() + "/Z" + curBlock.getZ());
			} 
			else {
				if (curBlock.equals(wildcardB))
					sendMsgToClient(MessageType.PATHING,
							"YET skipping because wildcard was found at: X" + 
							curBlock.getX() + "/Y" + curBlock.getY() + "/Z" + 
							curBlock.getZ());
				else {
					sendMsgToClient(MessageType.PATHING,
							"Skipping because not allowed via sign found at: X" + 
							curBlock.getX() + "/Y" + curBlock.getY() + "/Z" + 
							curBlock.getZ());
				}
				  
				return null;
			}
		}

		// Only process this block once per item or we will get infinite loops!
		visitedB.add(curBlock); // Place this after the wildcard-return!

		Block findBlock;
		Block aboveBlock = curBlock.getRelative(BlockFace.UP);
		Material aboveType = aboveBlock.getType();
		
		findBlock = findNextEmptyComponentHelper(curBlock, aboveBlock, aboveType, BlockFace.NORTH, itemStack, itemIdStr);
		if (findBlock != null) {
			return findBlock;
		}
		findBlock = findNextEmptyComponentHelper(curBlock, aboveBlock, aboveType, BlockFace.EAST, itemStack, itemIdStr);
		if (findBlock != null) {
			return findBlock;
		}
		findBlock = findNextEmptyComponentHelper(curBlock, aboveBlock, aboveType, BlockFace.SOUTH, itemStack, itemIdStr);
		if (findBlock != null) {
			return findBlock;
		}
		findBlock = findNextEmptyComponentHelper(curBlock, aboveBlock, aboveType, BlockFace.WEST, itemStack, itemIdStr);
		if (findBlock != null) {
			return findBlock;
		}
		return null;
	}

	/**
	 * From the current block, check in a specific direction if the block
	 * in that direction is a component for IntoChest to continue the traversal
	 * path. 
	 * <p>
	 * Is a helper function for {@link #findNextEmptyComponent(Block, ItemStack, String)}
	 * which is called recursively in order to traverse the possible IntoChest
	 * paths.
	 * 
	 * @param curBlock  the current block we are pointing at
	 * @param aboveBlock  the block above the current block
	 * @param aboveBlockType  the type of the aboveBlock
	 * @param direction  the direction to check for a block that is a component
	 * @param itemStack  object for item stack sitting on crafting table
	 * @param itemIdStr  identifier string of the item stack
	 * @return block that is found to be an end component
	 */
	private Block findNextEmptyComponentHelper(Block curBlock, 
																	Block aboveBlock,
																	Material aboveBlockType,
																	BlockFace direction, 
																	ItemStack itemStack, 
																	String itemIdStr) {
		Block newBlock;
		Material newBlockType;
		Block findBlock;
		Material findBlockType;
		
		newBlock = curBlock.getRelative(direction);
		newBlockType = newBlock.getType();
		
		if (isBlockTypeAComponent(newBlockType)) {
			findBlock = findNextEmptyComponent(newBlock, itemStack, itemIdStr);
			if (findBlock != null)
				return findBlock;
		} 
		else if (newBlockType == Material.AIR) { // Down one step
			findBlock = newBlock.getRelative(BlockFace.DOWN);
		  findBlockType = findBlock.getType();
			if (isBlockTypeAComponent(findBlockType)) {
				findBlock = findNextEmptyComponent(findBlock, itemStack, itemIdStr);
				if (findBlock != null)
					return findBlock;
			}
		} 
		else if (aboveBlockType == Material.AIR) { // Up one step
			findBlock = aboveBlock.getRelative(direction);
			findBlockType = findBlock.getType();
			if (isBlockTypeAComponent(findBlockType)) {
				findBlock = findNextEmptyComponent(findBlock, itemStack, itemIdStr);
				if (findBlock != null)
					return findBlock;
			}
		}
		return null;
	}
	
	/**
	 * Is a block's type a component that is valid for traversing an IntoChest
	 * path?
	 * <p>
	 * Valid types:
	 * <br>
	 * <ul>
	 * <li>Chest
	 * <li>Dispenser
	 * <li>Redstone Wire
	 * <li>Sign Post
	 * <li>Wall Sign
	 * </ul>
	 * <p>
	 * @param blockType  The type of the block to check
	 * @return true if the block has a type that is a valid component.
	 */
	protected boolean isBlockTypeAComponent(Material blockType) {
		if (blockType == Material.CHEST || 
				blockType == Material.DISPENSER || 
			  blockType == Material.REDSTONE_WIRE || 
				blockType == Material.SIGN_POST || 
				blockType == Material.WALL_SIGN) {
			return true;
		}
		else {
			return false;
		}
	}
	
	/**
	 * Is this block an end component where items may be dumped into?
	 * <p>
	 * Currently only chests and dispensers are end components.
	 * <p>
	 * @param block  The block we are checking
	 * @return true if the block is an end component
	 */
	protected boolean isBlockAnEndComponent(Block block) {
		Material curBlockType = block.getType();
		if (curBlockType == Material.CHEST || 
				curBlockType == Material.DISPENSER) {
			return true;
		}
		else 
		{
			return false;
		}
	}
	
	/**
	 * Is this block a filtering component for items?
	 * <p>
	 * Currently only standing signs and wall signs are filtering components.
	 * <p>
	 * @param block  The block we are checking
	 * @return true if the block is a filtering component
	 */
	protected boolean isBlockAFilterComponent(Block block) {
		Material curBlockType = block.getType();
		if (curBlockType == Material.SIGN_POST || 
				curBlockType == Material.WALL_SIGN) {
			return true;
		}
		else 
		{ 
			return false;
		}
	}
	
	/**
	 * Check if the item that's sitting on the crafting table is allowed to pass
	 * through a sign filter.
	 * <p>
	 * The sign can include text that matches an item through its item identifier
	 * (decimal ID and data value delimited by a pipe), decimal ID, exact name, 
	 * short name, or group name.
	 * 
	 * @param sign  the sign that is checked against the item sitting on crafting table
	 * @param typeId  item decimal ID (Example: 15)
	 * @param dataVal  item data value (Example: 2)
	 * @param itemIdStr  item identifier (Example: 15|2)
	 * @return true if the item is allowed to pass through the sign filter.
	 */
	protected boolean isAllowed(final Sign sign, 
			                      final int typeId, 
			                      final int dataVal,
			                      final String itemIdStr) {
		boolean retVal = false;
		boolean filterHasDataVal = false;
		char firstChar = ' ';
		boolean isfirstCharADigit = false;
		
		sendMsgToClient(MessageType.FILTER, getSignItems(sign));
		
		for (String filterStr : (sign.getLine(0) +
		                         sign.getLine(1) + 
					                   sign.getLine(2) + 
							               sign.getLine(3)).toLowerCase().split(",")) {
			filterStr = filterStr.trim();

			if (filterStr.contains(DELIMITER)) {
				filterHasDataVal = true;
			}
			else {
				filterHasDataVal = false;
			}
			
		  //Don't process if the term is an empty string
			if (filterStr.length() == 0) {
				continue;
			}
			
			firstChar = filterStr.charAt(0);
			isfirstCharADigit = (firstChar >= '0' && firstChar <= '9');
			
			//Filter is a decimal identifier, not a lexical name
			if (isfirstCharADigit) {
				//Filter is in the form of an item identifier for exact matching: "10|1"
			  if (filterHasDataVal) {
					if (filterStr.equals(itemIdStr)) { //Item decimal number and data value
						sendMsgToClient(MessageType.FILTER, "Fit via id: " + filterStr);
						retVal = true;
					}
				}
			  //Filter only has the item's decimal ID: "10"
			  else {
			  	if (filterStr.equals(String.valueOf(typeId))) { //Item decimal number
			  		sendMsgToClient(MessageType.FILTER, "Fit via id of a group: " + typeId);
			  		retVal = true;
			  	}
			  }
			}
			//Filter is a lexical name
			else {
				if (filterStr.equals(ITEMNAMES.get(itemIdStr))) { //Item exact name
					sendMsgToClient(MessageType.FILTER, "Fit via exact name: " + 
							filterStr);
					retVal = true; 
				} 
				else if (GRPDATAVALUES.containsKey(filterStr)) { //Group name that includes item
					ArrayList<String> valuesList = GRPDATAVALUES.get(filterStr);
					if (valuesList.contains(itemIdStr)) {
						sendMsgToClient(MessageType.FILTER, "Fit via group name: " + 
								filterStr);
						retVal = true;
					}	
				}
				else if (filterStr.equals(SHORTITEMNAMES.get(itemIdStr))) { //Item short name
					sendMsgToClient(MessageType.FILTER, "Fit via short name: " + 
							filterStr + " -> " + ITEMNAMES.get(itemIdStr));
				  retVal = true;
				}
			}
			
		  // Wildcard sign, means "if nothing else matches"
			if (!retVal && filterStr.matches("^\\*$")) { 
				sendMsgToClient(MessageType.FILTER, 
						"We have a wildcard! X" + sign.getBlock().getX() + "/Y" + 
						sign.getBlock().getY() + "/Z" + sign.getBlock().getZ());
				if (wildcardB == null) {
					// On the first run stop processing the wildcard and do the rest first.
					wildcardB = sign.getBlock();
					retVal = false;
				} 
				else {
					// Then, on the second run, after everything else, do the wildcard.
					wildcardB = null;
					retVal = true;
				}
			}
		}
		return retVal;
	}
	
	/**
	 * Gets a string that includes a list of items that are allowed to pass
	 * through a sign and a list of invalid terms from the sign that could
	 * not be resolved to an item.
	 * 
	 * @param sign  the sign to get its allowed items and invalid terms
	 * @return the output string
	 */
	protected String getSignItems(Sign sign) {
		String retStr = "";
		ArrayList<String> allowedList = new ArrayList<String>();
		ArrayList<String> invalidList = new ArrayList<String>();
		
		allowedList = getAllowedItemsOfSign(sign, invalidList);
		
		if (allowedList.size() > 0) {
			retStr = "\nAllowed: ";
			for (String str : allowedList) {
				retStr += str + ", ";
			}
			retStr = retStr.substring(0,retStr.length()-2);
		}
		
		if (invalidList.size() > 0) {
			retStr += "\nInvalid: ";
			for (String str : invalidList) {
				retStr += str + ", ";
			}
			retStr = retStr.substring(0,retStr.length()-2);
		}

		if (retStr.length() == 0) {
			retStr = "This sign has no filters specified. No items allowed.";
		}
		return retStr;
	}
	
	/**
	 * Gets an {@code ArrayList} of allowed items from a sign. Also will set 
	 * param {@code invalidList} with an {@code ArrayList} of invalid terms from
	 * the sign that could not be resolved to an item.
	 * 
	 * @param sign  the sign to get its allowed items and invalid terms
	 * @param invalidList  invalid list of terms
	 * @return allowed list of items
	 */
	protected ArrayList<String> getAllowedItemsOfSign(Sign sign, ArrayList<String> invalidList) {
		ArrayList<String> allowedList = new ArrayList<String>();
		String signStr;
		boolean filterHasDataVal = false;
		char firstChar = ' ';
		boolean isfirstCharADigit = false;
		int dataValCtr = 0;
		String tempFilterStr = "";
		boolean itemAllowedFound = false;
		String tempKey = "";
		
		signStr = sign.getLine(0) +
        sign.getLine(1) + 
        sign.getLine(2) + 
        sign.getLine(3).toLowerCase();	
		
		//Process each term in the sign
		for (String filterStr : signStr.split(",")) {
			filterStr = filterStr.trim();
			
			//Don't process if the term is an empty string
			if (filterStr.length() == 0) {
				continue;
			}

			if (filterStr.contains(DELIMITER)) {
				filterHasDataVal = true;
			}
			else {
				filterHasDataVal = false;
			}
			
			firstChar = filterStr.charAt(0);
			isfirstCharADigit = (firstChar >= '0' && firstChar <= '9');
			
			if (isfirstCharADigit) {
			  if (filterHasDataVal) {
					if (ITEMNAMES.containsKey(filterStr)) { //via exact item item identifier
						allowedList.add(ITEMNAMES.get(filterStr));
					}
					else {
						invalidList.add(filterStr);
					}
				}
			  else { //via ID of a group
			  	dataValCtr = 0;
			  	itemAllowedFound = false;
			  	tempFilterStr = filterStr + DELIMITER + dataValCtr;
			  	
			  	//Add every item that is contained in a decimal ID 
			  	while (ITEMNAMES.containsKey(tempFilterStr)) {
			  		allowedList.add(ITEMNAMES.get(tempFilterStr));
			  		dataValCtr++;
			  		tempFilterStr = filterStr + DELIMITER + dataValCtr;
			  		itemAllowedFound = true;
			  	}
			  	
			  	if (!itemAllowedFound) {
			  		invalidList.add(filterStr);
			  	}
			  }
			}
			else if (!isfirstCharADigit) { 
				if (ITEMNAMES.containsValue(filterStr)) { //via exact name
					allowedList.add(filterStr);
				}
				else if (GRPDATAVALUES.containsKey(filterStr)) { //via group name
					//Add every item that is contained in a group name
					for (String key : GRPDATAVALUES.get(filterStr)) {
						allowedList.add(ITEMNAMES.get(key));
					}
				}
				else if (SHORTITEMNAMES.containsValue(filterStr)) { //via short name
					//Find the key (item identifier) for the value (the short name).
					//Have to loop because key is not indexed. Once key is found we can
					//use the same key to get the full exact name from ITEMNAMES.
					for (Entry<String,String> entry : SHORTITEMNAMES.entrySet()) {
						if (Objects.equals(filterStr, entry.getValue())) {
							tempKey = entry.getKey();
							break;
						}
					}
					allowedList.add(ITEMNAMES.get(tempKey));
				}
				else {
					invalidList.add(filterStr);
				}
			}
		}	
		return allowedList;
	}

	/**
	 * Check if the contents of an end component is full or has no more space
	 * to dump the item(s).
	 * <p>
	 * @param endCompContents  contents of an end component
	 * @param check  the stack of the item we want to dump
	 * @return true if end component cannot take the item(s) we want to dump
	 */
	protected boolean isEndComponentFull(ItemStack[] endCompContents, 
																		 ItemStack check) {
		if (endCompContents == null) {
			return false;
		}
		
		int amt = check.getAmount();
		int maxStack = check.getMaxStackSize(); //Not all items stack up to 64
		int checkTypeId = check.getTypeId();
		byte checkDataValue = check.getData().getData();
		
		sendMsgToClient(MessageType.GENERAL, "Quantity to stack: " + amt);
		for (ItemStack item : endCompContents) {	
			//Empty slot -> Chest/Dispenser is not full for sure. :)
			if (item == null || item.getAmount() == 0) {
				sendMsgToClient(MessageType.GENERAL, "Stackable into empty slot");
				return false;
			}
			
			//If item is unstackable, it can never stack on an existing stack.
			if (maxStack == 1) {
				continue;
			}
		  //Contains a stack of item of same type, check if we can fit everything
			//in that stack. 
			if (item.getTypeId() == checkTypeId) { 
				if (item.getData().getData() == checkDataValue) {
					amt -= maxStack - item.getAmount();
					if (amt <= 0) {
						sendMsgToClient(MessageType.GENERAL, "Stackable onto existing item stack");
					  return false;
					}
				}
			}
		}
		sendMsgToClient(MessageType.GENERAL, "Chest/dispenser does not have room to take the item");
		return true;
	}

	/**
	 * Gets the item contents of the chest(s) or dispenser.
	 * <p>
	 * @param block  end component to check contents for
	 * @return array of item contents
	 */
	protected ItemStack[] getContents(Block block) {
		BlockState blockState = block.getState();
		if (blockState instanceof Chest) {
			Chest chest = (Chest) blockState;
			Chest otherChest = getOtherChest(block);
			if (otherChest == null)
				return chest.getInventory().getContents();
			else {
				ArrayList<ItemStack> itemContentsList = new ArrayList<ItemStack>();
				if (otherChest.getX() < block.getX() || 
						otherChest.getZ() < block.getZ()) {
					itemContentsList.addAll(Arrays.asList(otherChest.getInventory().getContents()));
					itemContentsList.addAll(Arrays.asList(chest.getInventory().getContents()));
				} 
				else {
					itemContentsList.addAll(Arrays.asList(chest.getInventory().getContents()));
					itemContentsList.addAll(Arrays.asList(otherChest.getInventory().getContents()));
				}
				return itemContentsList.toArray(new ItemStack[itemContentsList.size()]);
			}
		} 
		else if (blockState instanceof Dispenser) {
			return ((Dispenser) blockState).getInventory().getContents();
		} 
		else {
			logger.warning("Unknown BlockState: " + blockState);
			return null;
		}
	}

	/**
	 * Adds the item sitting on the crafting table into the chest(s) or dispenser.
	 * 
	 * @param block  end component to check contents for
	 * @param stack  stack of item(s) sitting on crafting table
	 */
	protected void addContents(Block block, ItemStack stack) {
		BlockState blockState = block.getState();
		if (blockState instanceof Chest) {
			Chest chest = (Chest) blockState;
			Chest otherChest = getOtherChest(block);
			if (otherChest == null)
				chest.getInventory().addItem(stack);
			else if ((otherChest.getX() < block.getX()) || 
					     (otherChest.getZ() < block.getZ())) {
				if (isEndComponentFull(otherChest.getInventory().getContents(), stack))
					chest.getInventory().addItem(stack);
				else
					otherChest.getInventory().addItem(stack);
			} 
			else {
				if (isEndComponentFull(chest.getInventory().getContents(), stack))
					otherChest.getInventory().addItem(stack);
				else
					chest.getInventory().addItem(stack);
			}
		} 
		else if (blockState instanceof Dispenser) {
			((Dispenser) blockState).getInventory().addItem(stack);
		} 
		else {
			logger.warning("Unknown BlockState: " + blockState);
		}
	}

	/**
	 * Gets the other chest of a chest block (if there is one).
	 * <p>
	 * Chests can be placed as one chest or two chests.
	 * <p>
	 * @param block  the chest we have already identified
	 * @return the chest object that is the other chest
	 */
	protected static Chest getOtherChest(Block block) {
		Chest otherChest = null;
		
		if (block == null) {
			return null;
		}
		
		if (isBlockOtherChest(block, BlockFace.NORTH, otherChest)) {
			return otherChest;
		}
		else if (isBlockOtherChest(block, BlockFace.WEST, otherChest)) {
			return otherChest;
		}
		else if (isBlockOtherChest(block, BlockFace.SOUTH, otherChest)) {
			return otherChest;
		}
		else if (isBlockOtherChest(block, BlockFace.EAST, otherChest)) {
			return otherChest;
		}
		else {
			return null;
		}
	}
	
	/**
	 * Helper fxn to check if a certain face of a block is the other chest to 
	 * a chest.
	 * <p>
	 * @param block  chest block we already have identified
	 * @param face  what face should we check
	 * @param otherChest  object of otherChest that is to be assigned if the 
	 *                    other chest has been found
	 * @return true if the otherChest was found at the face specified
	 */
	private static boolean isBlockOtherChest(Block block, BlockFace face, Chest otherChest) {
		if (block.getRelative(face).getType() == Material.CHEST) {
			otherChest = (Chest) block.getRelative(face).getState();
			return true;
		}
		else {
			otherChest = null;
			return false;
		}
	}
	
	/**
	 * Attempts to send a message to the client console. If the message type is
	 * not set to be outputted, the message will not be sent.
	 * <p>
	 * @param msgType  the message type
	 * @param text  the message string
	 */
	protected void sendMsgToClient(MessageType msgType, String text) {
		boolean doSendMsg = false;
		
		doSendMsg = canSendMsg(msgType);	
		if (doSendMsg) {
			if (clientConsole != null) {
				clientConsole.sendMessage(ChatColor.RED + pluginName + ": " + 
			  ChatColor.GRAY + text);
			}
			else
			{
				logger.info(pluginName + ": " + text);
			}
		}
		
		//sendMsgToLogger(msgType, text); //TODO: Remove
	}
	
	/**
	 * Attempts to send a message to the logger. If the message type is not
	 * set to be outputted, the message will not be sent.
	 * <p>
	 * @param msgType  the message type
	 * @param text  the message string
	 */
	protected void sendMsgToLogger(MessageType msgType, String text) {
		boolean doSendMsg = false;
		
		doSendMsg = canSendMsg(msgType);
		if (doSendMsg) {
			logger.info(pluginName + ": " + text);
		}
	}
	
	/**
	 * Checks if the message type is set to be outputted.
	 * <p>
	 * @param msgType  the message type
	 * @return true if the message type can be outputted.
	 */
	private boolean canSendMsg(MessageType msgType) {
		boolean doSendMsg = false;
		
		switch (msgType) {
			case ONCOMMAND:             //Command-level messages should always be on
				doSendMsg = true;
			case GENERAL:
				if (isGeneralDebugOn) {
					doSendMsg = true;
				}
				break;
			case RUNTIME:
				if (isRuntimeDebugOn) {
					doSendMsg = true;
				}
				break;
			case FILTER:
				if (isFilterDebugOn) {
					doSendMsg = true;
				}
				break;
			case PATHING:
				if (isPathingDebugOn) {
					doSendMsg = true;
				}
				break;
			default:
				break;
		}
		return doSendMsg;
	}
	
	/**
	 * Event handler when a player interacts with a sign or an item in his hand.
	 * If player has initiated an item inspection by calling the command for it,
	 * will return what items are allowed/invalid for a sign or if a sign wasn't 
	 * clicked on, the info on valid identifiers for the item in their hand.
	 * 
	 * @param event  data for when player does an interaction with left click
	 *               or right click.
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerInteract(PlayerInteractEvent event) {
		String playerName = event.getPlayer().getName();
		Sign sign;
		ItemStack itemStack;

		//If player did not initiate to inspect, don't bother to inspect
		if (!playersChecking.containsKey(playerName)) {
			return;
		}
		
		//If player clicked on a sign return allowed/invalid items on the sign
		if ((event.getClickedBlock() != null) &&
				(isBlockAFilterComponent(event.getClickedBlock()))) {
			sign = (Sign) event.getClickedBlock().getState();
			sendMsgToClient(MessageType.ONCOMMAND, getSignItems(sign));
			
			if (playersChecking.get(playerName).equals("once")) {
				playersChecking.remove(playerName);
			}
		}
		//Otherwise if they have an item in their hand return info for the item.
		else if (event.getItem() != null){
		  itemStack = event.getItem();
		  sendMsgToClient(MessageType.ONCOMMAND, inspectItem(itemStack));
		  if (playersChecking.get(playerName).equals("once")) {
				playersChecking.remove(playerName);
			}
		}
	}
	
	/**
	 * Looks at an item and returns text of what are valid identifiers for it
	 * 
	 * @param itemStack  the item to inspect
	 * @return text of the item's valid identifiers
	 */
	private String inspectItem(ItemStack itemStack) {
		int itemId;
		int itemDataVal;
		String itemIdStr = "";
		String exactName = "";
		String shortName = "";
		String groupName = "";
		StringBuilder retStr = new StringBuilder();
		
		//Get the identifiers of the item
		itemId = itemStack.getTypeId();                 //Item ID
		itemDataVal = itemStack.getData().getData();    //Item data value
		itemIdStr = itemId + DELIMITER + itemDataVal;   //Item exact identifier
	
		if (ITEMNAMES.containsKey(itemIdStr)) {         //Exact name
			exactName = ITEMNAMES.get(itemIdStr);		
		}
	
		if (SHORTITEMNAMES.containsKey(itemIdStr)) {    //Short name
			shortName = SHORTITEMNAMES.get(itemIdStr);
		}
		
		for (Entry<String,ArrayList<String>> entry : GRPDATAVALUES.entrySet()) {
			if (entry.getValue().contains(itemIdStr)) {   //Group name(s)
				groupName += entry.getKey() + ", ";
			}
		}
		if (groupName.length() > 2) {
			groupName = groupName.substring(0, groupName.length()-2);
		}
		
		//Add valid identifiers to the text
		retStr.append("The following values are valid filter terms for " + 
				"your item in hand: " + "\n");
		if (!exactName.isEmpty()) {
			retStr.append("Exact name: " + exactName + "\n");
		}
		if (!shortName.isEmpty()) {
			retStr.append("Short name: " + shortName + "\n");
		}
		if (!groupName.isEmpty()) {
			retStr.append("Group name(s): " + groupName + "\n");
		}
		if (!itemIdStr.isEmpty()) {
			retStr.append("Item exact identifier: " + itemIdStr + "\n");
		}
		if (itemId >= 0) {
			retStr.append("Item group identifier: " + itemId + "\n");
		}
		
		return retStr.toString();
	}
}
