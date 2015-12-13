package atpx.minecraft;

import java.util.*;

public interface DataValues {

	public static final List<Integer> NOTSTACKABLE = 
			Collections.unmodifiableList(
				Arrays.asList(
						26, 63, 64, 68, 71, 256, 257, 258, 259, 261, 267, 268, 269, 270, 271, 272, 273, 274, 275, 276, 277, 278, 279, 282, 283, 284,
						285, 286, 290, 291, 292, 293, 294, 298, 299, 300, 301, 302, 303, 304, 305, 306, 307, 308, 309, 310, 311, 312, 313, 314, 315,
						316, 317, 323, 324, 325, 326, 327, 328, 329, 330, 333, 335, 342, 343, 346, 354, 355, 358, 359, 373,
						2256, 2257, 2258, 2259, 2260, 2261, 2262, 2263, 2264, 2265, 2266
				)
			);

	public static final List<Integer> TYPEDEPENDANTSTACKABLE = 
			Collections.unmodifiableList(
				Arrays.asList(
						17, // Wood
						35, // Wool
						43, // Double Slabs, should not happen
						44, // Single Slabs
						98  // Stone Brick
				)
		);

	/*
    public static final Map<String, String> ITEMNAMES = Collections.unmodifiableMap(
	 
			new HashMap<String, String>() {{
				put("SHEARS","Shears");
			}}
	);
	*/
			
	public static final Map<String, String> DATAVALUES = 
			Collections.unmodifiableMap(
				new HashMap<String, String>() {{
					put("oak wood","17|0");
					put("spruce wood","17|1");
					put("birch wood","17|2");
					put("jungle wood","17|3");
					put("shears","359|0");
					/*put("air", 0);
					put("stone", 1);
					put("grass", 2);
					put("dirt", 3);
					put("cobblestone", 4);
					put("wooden plank", 5);
					put("sapling", 6);
					put("bedrock", 7);
					put("water", 8);
					put("stationary water", 9);
					put("lava", 10);
					put("stationary lava", 11);
					put("sand", 12);
					put("gravel", 13);
					put("gold ore", 14);
					put("iron ore", 15);
					put("coal ore", 16);
					put("wood", 17);
					put("leaves", 18);
					put("sponge", 19);
					put("glass", 20);
					put("lapis lazuli ore", 21);
					put("lapis lazuli block", 22);
					put("dispenser", 23);
					put("sandstone", 24);
					put("note block", 25);
					put("bed", 26);
					put("powered rail", 27);
					put("detector rail", 28);
					put("sticky piston", 29);
					put("cobweb", 30);
					put("tall grass", 31);
					put("dead bush", 32);
					put("piston", 33);
					put("piston extension", 34);
					put("wool", 35);
					put("block moved by piston", 36);
					put("dandelion", 37);
					put("rose", 38);
					put("brown mushroom", 39);
					put("red mushroom", 40);
					put("block of gold", 41);
					put("block of iron", 42);
					put("double slabs", 43);
					put("slabs", 44);
					put("brick block", 45);
					put("tnt", 46);
					put("bookshelf", 47);
					put("moss stone", 48);
					put("obsidian", 49);
					put("torch", 50);
					put("fire", 51);
					put("monster spawner", 52);
					put("wooden stairs", 53);
					put("chest", 54);
					put("redstone wire", 55);
					put("diamond ore", 56);
					put("block of diamond", 57);
					put("crafting table", 58);
					put("seeds", 59);
					put("farmland", 60);
					put("furnace", 61);
					put("burning furnace", 62);
					put("sign post d", 63);
					put("wooden door", 64);
					put("ladders", 65);
					put("rails", 66);
					put("cobblestone stairs", 67);
					put("wall sign", 68);
					put("lever", 69);
					put("stone pressure plate", 70);
					put("iron door", 71);
					put("wooden pressure plate", 72);
					put("redstone ore", 73);
					put("glowing redstone ore", 74);
					put("redstone torch (\"off\" state)", 75);
					put("redstone torch (\"on\" state)", 76);
					put("stone button", 77);
					put("snow", 78);
					put("ice", 79);
					put("snow block", 80);
					put("cactus", 81);
					put("clay block", 82);
					put("sugar cane", 83);
					put("jukebox", 84);
					put("fence", 85);
					put("pumpkin", 86);
					put("netherrack", 87);
					put("soul sand", 88);
					put("glowstone", 89);
					put("portal", 90);
					put("jack-o-lantern", 91);
					put("cake block", 92);
					put("redstone repeater (\"off\" state)", 93);
					put("redstone repeater (\"on\" state)", 94);
					put("locked chest", 95);
					put("trapdoor", 96);
					put("hidden silverfish", 97);
					put("stone bricks", 98);
					put("huge brown mushroom", 99);
					put("huge red mushroom", 100);
					put("iron bars", 101);
					put("glass pane", 102);
					put("melon", 103);
					put("pumpkin stem", 104);
					put("melon stem", 105);
					put("vines", 106);
					put("fence gate", 107);
					put("brick stairsd", 108);
					put("stone brick stairsd", 109);
					put("mycelium", 110);
					put("lily pad", 111);
					put("nether brick", 112);
					put("nether brick fence", 113);
					put("nether brick stairsd", 114);
					put("nether wart", 115);
					put("enchantment table", 116);
					put("brewing stand", 117);
					put("cauldron", 118);
					put("end portal", 119);
					put("end portal frame", 120);
					put("white stone", 121);
					put("iron shovel", 256);
					put("iron pickaxe", 257);
					put("iron axe", 258);
					put("flint and steel", 259);
					put("red apple", 260);
					put("bow", 261);
					put("arrow", 262);
					put("coal", 263);
					put("diamond", 264);
					put("iron ingot", 265);
					put("gold ingot", 266);
					put("iron sword", 267);
					put("wooden sword", 268);
					put("wooden shovel", 269);
					put("wooden pickaxe", 270);
					put("wooden axe", 271);
					put("stone sword", 272);
					put("stone shovel", 273);
					put("stone pickaxe", 274);
					put("stone axe", 275);
					put("diamond sword", 276);
					put("diamond shovel", 277);
					put("diamond pickaxe", 278);
					put("diamond axe", 279);
					put("stick", 280);
					put("bowl", 281);
					put("mushroom soup", 282);
					put("gold sword", 283);
					put("gold shovel", 284);
					put("gold pickaxe", 285);
					put("gold axe", 286);
					put("string", 287);
					put("feather", 288);
					put("gunpowder", 289);
					put("wooden hoe", 290);
					put("stone hoe", 291);
					put("iron hoe", 292);
					put("diamond hoe", 293);
					put("gold hoe", 294);
					put("seeds", 295);
					put("wheat", 296);
					put("bread", 297);
					put("leather cap", 298);
					put("leather tunic", 299);
					put("leather pants", 300);
					put("leather boots", 301);
					put("chain helmet", 302);
					put("chain chestplate", 303);
					put("chain leggings", 304);
					put("chain boots", 305);
					put("iron helmet", 306);
					put("iron chestplate", 307);
					put("iron leggings", 308);
					put("iron boots", 309);
					put("diamond helmet", 310);
					put("diamond chestplate", 311);
					put("diamond leggings", 312);
					put("diamond boots", 313);
					put("gold helmet", 314);
					put("gold chestplate", 315);
					put("gold leggings", 316);
					put("gold boots", 317);
					put("flint", 318);
					put("raw porkchop", 319);
					put("cooked porkchop", 320);
					put("paintings", 321);
					put("golden apple", 322);
					put("sign", 323);
					put("wooden door", 324);
					put("bucket", 325);
					put("water bucket", 326);
					put("lava bucket", 327);
					put("minecart", 328);
					put("saddle", 329);
					put("iron door", 330);
					put("redstone", 331);
					put("snowball", 332);
					put("boat", 333);
					put("leather", 334);
					put("milk", 335);
					put("clay brick", 336);
					put("clay", 337);
					put("sugar cane", 338);
					put("paper", 339);
					put("book", 340);
					put("slimeball", 341);
					put("minecart with chest", 342);
					put("minecart with furnace", 343);
					put("egg", 344);
					put("compass", 345);
					put("fishing rod", 346);
					put("clock", 347);
					put("glowstone dust", 348);
					put("raw fish", 349);
					put("cooked fish", 350);
					put("dye", 351);
					put("bone", 352);
					put("sugar", 353);
					put("cake", 354);
					put("bed", 355);
					put("redstone repeater", 356);
					put("cookie", 357);
					put("map", 358);
					put("shears", 359);
					put("melon (slice)", 360);
					put("pumpkin seeds", 361);
					put("melon seeds", 362);
					put("raw beef", 363);
					put("steak", 364);
					put("raw chicken", 365);
					put("cooked chicken", 366);
					put("rotten flesh", 367);
					put("ender pearl", 368);
					put("blaze rod", 369);
					put("ghast tear", 370);
					put("gold nugget", 371);
					put("nether wart", 372);
					put("potions", 373);
					put("glass bottle", 374);
					put("spider eye", 375);
					put("fermented spider eye", 376);
					put("blaze powder", 377);
					put("magma cream", 378);
					put("brewing stand", 379);
					put("cauldron", 380);
					put("eye of ender", 381);
					put("glistering melon", 382);
					put("13 disc", 2256);
					put("cat disc", 2257);
					put("blocks disc", 2258);
					put("chirp disc", 2259);
					put("far disc", 2260);
					put("mall disc", 2261);
					put("mellohi disc", 2262);
					put("stal disc", 2263);
					put("strad disc", 2264);
					put("ward disc", 2265);
					put("11 disc", 2266);*/
				}}
		);

	public static final Map<String, String> ITEMNAMES = 
	    new HashMap<String, String>();
  }
