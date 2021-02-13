package com.backsun.lod.objects;

import java.awt.Color;

import com.backsun.lod.util.enums.ColorDirection;
import com.backsun.lod.util.enums.LodLocation;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

/**
 * This object contains position
 * and color data for an LOD object.
 * 
 * @author James Seibel
 * @version 02-13-2021
 */
public class LodChunk
{
	/** how many different pieces of data are in one line */
	private static final int DATA_DELIMITER_COUNT = 28;

	/** This is what separates each piece of data in the toData method */
	public static final char DATA_DELIMITER = ',';
	
	public static final int WIDTH = 16;
	
	private static final int CHUNK_DATA_WIDTH = WIDTH;
	private static final int CHUNK_DATA_HEIGHT = WIDTH;
	
	private final int airBlockId = Block.getIdFromBlock(Block.getBlockFromName("air"));
	private final int waterBlockId = Block.getIdFromBlock(Block.getBlockFromName("water"));
	private final int waterColor = colorToInt(new Color(36, 50, 171));
	
	/**
	 * This is how many blocks are
	 * required at a specific y-value
	 * to constitute a LOD point
	 */
	private final int LOD_BLOCK_REQ = 16;
	// the max number of blocks per layer = 64 (8*8)
	// since each layer is 1/4 the chunk
	
	
	/** The x coordinate of the chunk. */
	public int x;
	/** The z coordinate of the chunk. */
	public int z;
	
	// each short is the height of
	// that 8th of the chunk.
	public short top[];
	public short bottom[];
	
	/** The average color of each 6 cardinal directions */
	public Color colors[];
	
	
	
	
	
	
	
	
	
	//==============//
	// constructors //
	//==============//
	
	/**
	 * Create an empty LodChunk
	 */
	public LodChunk()
	{
		x = 0;
		z = 0;
		
		top = new short[4];
		bottom = new short[4];
		colors = new Color[6];
		
		// by default have the colors invisible
		for(ColorDirection dir : ColorDirection.values())
		{
			colors[dir.value] = new Color(0, 0, 0, 0);
		}
	}
	
	/**
	 * Creates an LodChunk from the string
	 * generated by the toData method.
	 * 
	 * @throws IllegalArgumentException if the data isn't valid to create a LodChunk
	 * @throws NumberFormatException if the data can't be converted into an int at any point
	 */
	public LodChunk(String data) throws IllegalArgumentException, NumberFormatException
	{
		/*
		 * data format:
		 * x, z, top data, bottom data, rgb color data
		 * 
		 * example:
		 * 5,8, 4,4,4,4, 0,0,0,0, 255,255,255, 255,255,255, 255,255,255, 255,255,255, 255,255,255, 255,255,255,
		 */
		
		// make sure there are the correct number of entries
		// in the data string (28)
		int count = 0;
		
		for(int i = 0; i < data.length(); i++)
		{
			if(data.charAt(i) == DATA_DELIMITER)
			{
				count++;
			}
		}
		
		if(count != DATA_DELIMITER_COUNT)
		{
			throw new IllegalArgumentException("LodChunk constructor givin an invalid string. The data given had " + count + " delimiters when it should have had " + DATA_DELIMITER_COUNT + ".");
		}
		
		
		
		// index we will use when going through the String
		int index = 0;
		int lastIndex = 0;
		
		
		
		// x and z position
		index = data.indexOf(DATA_DELIMITER, 0);
		x = Integer.parseInt(data.substring(0,index));
		
		lastIndex = index;
		index = data.indexOf(DATA_DELIMITER, lastIndex + 1);
		z = Integer.parseInt(data.substring(lastIndex+1,index));
		
		
		
		// top
		top = new short[4];
		for(LodLocation loc : LodLocation.values())
		{
			lastIndex = index;
			index = data.indexOf(DATA_DELIMITER, lastIndex + 1);
			
			top[loc.value] = Short.parseShort(data.substring(lastIndex+1,index));
		}
		
		
		// bottom
		bottom = new short[4];
		for(LodLocation loc : LodLocation.values())
		{
			lastIndex = index;
			index = data.indexOf(DATA_DELIMITER, lastIndex + 1);
			
			bottom[loc.value] = Short.parseShort(data.substring(lastIndex+1,index));
		}
		
		
		// color
		colors = new Color[6];
		for(ColorDirection dir : ColorDirection.values())
		{
			int red = 0;
			int green = 0;
			int blue = 0;
			
			// get r,g,b
			for(int i = 0; i < 3; i++)
			{
				lastIndex = index;
				index = data.indexOf(DATA_DELIMITER, lastIndex + 1);
				
				String raw = "";
				switch(i)
				{
				case 0:
					raw = data.substring(lastIndex+1,index);
					red = Short.parseShort(raw);
					break;
				case 1:
					raw = data.substring(lastIndex+1,index);
					green = Short.parseShort(raw);
					break;
				case 2:
					raw = data.substring(lastIndex+1,index);
					blue = Short.parseShort(raw);
					break;
				}
			}
			
			colors[dir.value] = new Color(red, green, blue);
		}
	}
	
	/**
	 * Illegal argument is thrown if either the
	 * chunk or world is null. The reason the world
	 * can't be null is because it's required to determine
	 * a block's color.
	 * @throws IllegalArgumentException
	 */
	public LodChunk(Chunk chunk, World world) throws IllegalArgumentException
	{
		if(chunk == null)
		{
			throw new IllegalArgumentException("LodChunk constructor given a null chunk");
		}
		if(world == null)
		{
			throw new IllegalArgumentException("LodChunk constructor given a null world");
		}
		
		
		x = chunk.x;
		z = chunk.z;
		
		top = new short[4];
		bottom = new short[4];
		colors = new Color[6];
		
		// generate the top and bottom points of this LOD
		for(LodLocation loc : LodLocation.values())
		{
			top[loc.value] = generateLodSection(chunk, true, loc);
			bottom[loc.value] = generateLodSection(chunk, false, loc);
		}
		
		// determine the average color for each direction
		for(ColorDirection dir : ColorDirection.values())
		{
			colors[dir.value] = generateLodColorSection(chunk, world, dir);
		}
	}
	
	
	
	
	
	
	
	//=====================//
	// constructor helpers //
	//=====================//
	
	
	/**
	 * If invalid/null/empty chunks are given 
	 * crashes may occur.
	 */
	public short generateLodSection(Chunk chunk, boolean getTopSection, LodLocation lodLoc)
	{
		// should have a length of 16
		// (each storage is 16x16x16 and the
		// world height is 256)
		ExtendedBlockStorage[] data = chunk.getBlockStorageArray();
		
		
		
		int startX = 0;
		int endX = 0;
		
		int startZ = 0;
		int endZ = 0;
		
		// determine where we should look in this
		// chunk
		switch(lodLoc)
		{
			case NE:
				// -N
				startZ = 0;
				endZ = (CHUNK_DATA_WIDTH / 2) - 1;
				// +E
				startX = CHUNK_DATA_WIDTH / 2;
				endX = CHUNK_DATA_WIDTH - 1;
				break;
				
			case SE:
				// +S
				startZ = CHUNK_DATA_WIDTH / 2;
				endZ = CHUNK_DATA_WIDTH;
				// +E
				startX = CHUNK_DATA_WIDTH / 2;
				endX = CHUNK_DATA_WIDTH;
				break;
				
			case SW:
				// +S
				startZ = CHUNK_DATA_WIDTH / 2;
				endZ = CHUNK_DATA_WIDTH;
				// -W
				startX = 0;
				endX = (CHUNK_DATA_WIDTH / 2) - 1;
				break;
				
			case NW:
				// -N
				startZ = 0;
				endZ = CHUNK_DATA_WIDTH / 2;
				// -W
				startX = 0;
				endX = CHUNK_DATA_WIDTH / 2;
				break;
		}
		
		
		if(getTopSection)
			return determineTopPoint(data, startX, endX, startZ, endZ);
		else
			return determineBottomPoint(data, startX, endX, startZ, endZ);
	}
	
	private short determineBottomPoint(ExtendedBlockStorage[] data, int startX, int endX, int startZ, int endZ)
	{
		// search from the bottom up
		for(int i = 0; i < data.length; i++)
		{
			for(int y = 0; y < CHUNK_DATA_HEIGHT; y++)
			{
				
				if(isLayerValidLodPoint(data, startX, endX, startZ, endZ, i, y))
				{
					// we found
					// enough blocks in this
					// layer to count as an
					// LOD point
					return (short) (y + (i * CHUNK_DATA_HEIGHT));
				}
				
			} // y
		} // data
		
		
		// we never found a valid LOD point
		return -1;
	}
	
	private short determineTopPoint(ExtendedBlockStorage[] data, int startX, int endX, int startZ, int endZ)
	{
		// search from the top down
		for(int i = data.length - 1; i >= 0; i--)
		{
			for(int y = CHUNK_DATA_WIDTH - 1; y >= 0; y--)
			{
				if(isLayerValidLodPoint(data, startX, endX, startZ, endZ, i, y))
				{
					// we found
					// enough blocks in this
					// layer to count as an
					// LOD point
					return (short) (y + (i * CHUNK_DATA_HEIGHT));
				}
			} // y
		} // data
		
		
		
		// we never found a valid LOD point
		return -1;
	}
	
	/**
	 * Is the layer between the given X, Z, and dataIndex
	 * values a valid LOD point?
	 */
	private boolean isLayerValidLodPoint(
			ExtendedBlockStorage[] data, 
			int startX, int endX, 
			int startZ, int endZ, 
			int dataIndex, int y)
	{
		// search through this layer
		int layerBlocks = 0;
		
		for(int x = startX; x < endX; x++)
		{
			for(int z = startZ; z < endZ; z++)
			{
				if(data[dataIndex] == null)
				{
					// this section doesn't have any blocks,
					// it is not a valid section
					return false;
				}
				else
				{
					if(data[dataIndex].get(x, y, z) != null && Block.getIdFromBlock(data[dataIndex].get(x, y, z).getBlock()) != airBlockId)
					{
						// we found a valid block in
						// in this layer
						layerBlocks++;
						
						if(layerBlocks >= LOD_BLOCK_REQ)
						{
							return true;
						}
					}
				}
				
			} // z
		} // x
		
		return false;
	}
	
	
	
	private Color generateLodColorSection(Chunk chunk, World world, ColorDirection colorDir)
	{
		Minecraft mc =  Minecraft.getMinecraft();
		BlockColors bc = mc.getBlockColors();
		
		switch (colorDir)
		{
			case TOP:
				return generateLodColorVertical(chunk, colorDir, world, bc);
			case BOTTOM:
				return generateLodColorVertical(chunk, colorDir, world, bc);
				
			case N:
				return generateLodColorHorizontal(chunk, colorDir, world, bc);
			case S:
				return generateLodColorHorizontal(chunk, colorDir, world, bc);
				
			case E:
				return generateLodColorHorizontal(chunk, colorDir, world, bc);
			case W:
				return generateLodColorHorizontal(chunk, colorDir, world, bc);
		}
		
		return new Color(0, 0, 0, 0);
	}
	
	/**
	 * Only accepts TOP and BOTTOM as ColorPositions
	 */
	private Color generateLodColorVertical(Chunk chunk, ColorDirection colorDir, World world, BlockColors bc)
	{
		ExtendedBlockStorage[] data = chunk.getBlockStorageArray();
		
		int numbOfBlocks = 0;
		int red = 0;
		int green = 0;
		int blue = 0;
		
		boolean goTopDown = (colorDir == ColorDirection.TOP);
		
		
		// either go top down or bottom up
		int dataStart = goTopDown? data.length - 1 : 0;
		int dataMax = data.length; 
		int dataMin = 0;
		int dataIncrement = goTopDown? -1 : 1;
		
		int topStart = goTopDown? CHUNK_DATA_HEIGHT - 1 : 0;
		int topMax = CHUNK_DATA_HEIGHT;
		int topMin = 0;
		int topIncrement =  goTopDown? -1 : 1;
		
		for(int x = 0; x < CHUNK_DATA_WIDTH; x++)
		{
			for(int z = 0; z < CHUNK_DATA_WIDTH; z++)
			{
				boolean foundBlock = false;
				
				for(int di = dataStart; !foundBlock && di >= dataMin && di < dataMax; di += dataIncrement)
				{
					if(!foundBlock && data[di] != null)
					{
						for(int y = topStart; !foundBlock && y >= topMin && y < topMax; y += topIncrement)
						{
							int ci;
							if(Block.getIdFromBlock(data[di].get(x, y, z).getBlock()) == waterBlockId)
								// this is a special case since getColor on water generally returns white
								ci = waterColor;
							else
								ci = bc.getColor(data[di].get(x, y, z), world, new BlockPos(x,y,z));
							
							if(ci == 0)
							{
								// skip air or invisible blocks
								continue;
							}
							
							Color c = intToColor(ci);
							
							red += c.getRed();
							green += c.getGreen();
							blue += c.getBlue();
							
							numbOfBlocks++;
							
							
							// we found a valid block, skip to the
							// next x and z
							foundBlock = true;
						}
					}
				}
				
			}
		}
		
		if(numbOfBlocks == 0)
			numbOfBlocks = 1;
		
		red /= numbOfBlocks;
		green /= numbOfBlocks;
		blue /= numbOfBlocks;
		
		return new Color(red, green, blue);
	}

	private Color generateLodColorHorizontal(Chunk chunk, ColorDirection colorDir, World world, BlockColors bc)
	{
		ExtendedBlockStorage[] data = chunk.getBlockStorageArray();
		
		int numbOfBlocks = 0;
		int red = 0;
		int green = 0;
		int blue = 0;
		
		
		// these don't change since the over direction doesn't matter
		int overStart = 0;
		int overIncrement = 1;
		
		// determine which direction is "in"
		int inStart = 0;
		int inIncrement = 1;
		switch (colorDir)
		{
		case N:
			inStart = 0;
			inIncrement = 1;
			break;
		case S:
			inStart = CHUNK_DATA_WIDTH - 1;
			inIncrement = -1;
			break;
		case E:
			inStart = 0;
			inIncrement = 1;
			break;
		case W:
			inStart = CHUNK_DATA_WIDTH - 1;
			inIncrement = -1;
			break;
		default:
			// we were given an invalid position, return invisible.
			// this shouldn't happen and is mostly here to make the
			// compiler happy
			return new Color(0,0,0,0);
		}
		
		
		for (int di = 0; di < data.length; di++)
		{
			if (data[di] != null)
			{
				for (int y = 0; y < CHUNK_DATA_HEIGHT; y++)
				{
					boolean foundBlock = false;
					
					// over moves "over" the side of the chunk
					// in moves "into" the chunk until it finds a block
					
					for (int over = overStart; !foundBlock && over >= 0 && over < CHUNK_DATA_WIDTH; over += overIncrement)
					{
						for (int in = inStart; !foundBlock && in >= 0 && in < CHUNK_DATA_WIDTH; in += inIncrement)
						{
							int x = -1;
							int z = -1;
							
							// determine which should be X and Z							
							switch(colorDir)
							{
							case N:
								x = over;
								z = in;
								break;
							case S:
								x = over;
								z = in;
								break;
							case E:
								x = in;
								z = over;
								break;
							case W:
								x = in;
								z = over;
								break;
							default:
								// this will never happen, it would have
								// been caught by the switch before the loops
								break;
							}
							
							int ci;
							if(Block.getIdFromBlock(data[di].get(x, y, z).getBlock()) == waterBlockId)
								// this is a special case since getColor on water generally returns white
								ci = waterColor;
							else
								ci = bc.getColor(data[di].get(x, y, z), world, new BlockPos(x,y,z));
							
							if (ci == 0) {
								// skip air or invisible blocks
								continue;
							}
							
							Color c = intToColor(ci);
							
							red += c.getRed();
							green += c.getGreen();
							blue += c.getBlue();
							
							numbOfBlocks++;
							
							// we found a valid block, skip to the
							// next x and z
							foundBlock = true;
						}
					}
				}
				
			}
		}
		
		if(numbOfBlocks == 0)
			numbOfBlocks = 1;
		
		red /= numbOfBlocks;
		green /= numbOfBlocks;
		blue /= numbOfBlocks;
		
		return new Color(red, green, blue);
	}
	
	/**
	 * Convert a BlockColors int into a Color object.
	 */
	private Color intToColor(int num)
	{
		int filter = 0b11111111;
		
		int red = (num >> 16 ) & filter;
		int green = (num >> 8 ) & filter;
		int blue = num & filter;
		
		return new Color(red, green, blue);
	}
	
	/**
	 * Convert a Color into a BlockColors object.
	 */
	private int colorToInt(Color color)
	{
		return color.getRGB();
	}
	
	
	
	
	
	
	
	
	
	//========//
	// output //
	//========//
	
	
	
	/**
	 * Outputs all data in csv format
	 * with the given delimiter.
	 * <br>
	 * Exports data in the form:
	 * <br>
	 * x, z, top data, bottom data, rgb color data
	 * 
	 * <br>
	 * example output:
	 * <br>
	 * 5,8, 4,4,4,4, 0,0,0,0 255,255,255, 255,255,255, 255,255,255, 255,255,255, 255,255,255, 255,255,255,
	 */
	public String toData()
	{
		String s = "";
		
		s += Integer.toString(x) + DATA_DELIMITER +  Integer.toString(z) + DATA_DELIMITER;
		
		for(int i = 0; i < top.length; i++)
		{
			s += Short.toString(top[i]) + DATA_DELIMITER;
		}
		
		for(int i = 0; i < bottom.length; i++)
		{
			s += Short.toString(bottom[i]) + DATA_DELIMITER;
		}
		
		for(int i = 0; i < colors.length; i++)
		{
			s += Integer.toString(colors[i].getRed()) + DATA_DELIMITER + Integer.toString(colors[i].getGreen()) + DATA_DELIMITER + Integer.toString(colors[i].getBlue()) + DATA_DELIMITER;
		}
		
		return s;
	}
	
	
	@Override
	public String toString()
	{
		String s = "";
		
		s += "x: " + x + " z: " + z + "\t";
		
//		s += "top: ";
//		for(int i = 0; i < top.length; i++)
//		{
//			s += top[i] + " ";
//		}
//		s += "\t";
		
//		s += "bottom: ";
//		for(int i = 0; i < bottom.length; i++)
//		{
//			s += bottom[i] + " ";
//		}
//		s += "\t";
		
//		s += "colors ";
//		for(int i = 0; i < colors.length; i++)
//		{
//			if(colors[i] != null)
//				s += "(" + colors[i].getRed() + ", " + colors[i].getGreen() + ", " + colors[i].getBlue() + "), ";
//		}
		
		s += "(" + colors[ColorDirection.TOP.value].getRed() + ", " + colors[ColorDirection.TOP.value].getGreen() + ", " + colors[ColorDirection.TOP.value].getBlue() + "), ";
		
		return s;
	}
}
