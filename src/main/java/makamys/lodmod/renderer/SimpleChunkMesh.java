package makamys.lodmod.renderer;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;

import makamys.lodmod.LODMod;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;
import net.minecraft.world.chunk.Chunk;

public class SimpleChunkMesh extends Mesh {
	
	private FloatBuffer vertices;
	
	public static int usedRAM;
	public static int instances;
	
	public SimpleChunkMesh(Chunk target) {
		int divisions = 4;
		quadCount = divisions * divisions * 5;
		
		buffer = BufferUtils.createByteBuffer(4 * 6 * 7 * quadCount);
		vertices = buffer.asFloatBuffer();
		
		for(int divX = 0; divX < divisions; divX++) {
			for(int divZ = 0; divZ < divisions; divZ++) {
				IIcon icon = null;
				int color = 0xFFFFFFFF;
				int size = 16 / divisions;
				int y = 255;
				boolean foundWater = false;
				for(y = 255; y > 0; y--) {
					int xOff = divX * size;
					int zOff = divZ * size;
					Block block = target.getBlock(xOff, y, zOff);
					
					float offX = target.xPosition * 16 + divX * size;
					float offY = y;
					float offZ = target.zPosition * 16 + divZ * size;
					
					if(!foundWater && block.getMaterial() == Material.water) {
						// TODO just add a face here, and keep the seabed
						foundWater = true;
						int meta = target.getBlockMetadata(xOff, y, zOff);
						IIcon waterIcon = block.getIcon(1, meta);
						int waterColor = block.colorMultiplier(Minecraft.getMinecraft().theWorld, target.xPosition * 16 + xOff, y, target.zPosition * 16 + zOff);
						waterColor |= 0xFF000000;
						
						addCube(offX, offY, offZ, size, size, size*4, waterIcon, waterColor);
						break;
					}
					
					if(block.isBlockNormalCube() && block.isOpaqueCube() && block.renderAsNormalBlock()) {
						int meta = target.getBlockMetadata(xOff, y, zOff);
						icon = block.getIcon(1, meta);
						color = block.colorMultiplier(Minecraft.getMinecraft().theWorld, target.xPosition * 16 + xOff, y, target.zPosition * 16 + zOff);
						color = (0xFF << 24) | ((color >> 16 & 0xFF) << 0) | ((color >> 8 & 0xFF) << 8) | ((color >> 0 & 0xFF) << 16);
						
						addCube(offX, offY, offZ, size, size, offY, icon, color);
						break;
					}
				}
			}
		}
		vertices.flip();
		
		usedRAM += buffer.limit();
        instances++;
	}
	
	private void addCube(float x, float y, float z, float sizeX, float sizeZ, float sizeY, IIcon icon, int color) {
		addFace(
				x + 0, y + 0, z + 0,
				x + 0, y + 0, z + sizeZ,
				x + sizeX, y + 0, z + sizeZ,
				x + sizeX, y + 0, z + 0,
				icon, color, 240
				);
		addFace(
			x + 0, y - sizeY, z + 0,
			x + 0, y + 0, z + 0,
			x + sizeX, y + 0, z + 0,
			x + sizeX, y - sizeY, z + 0,
			icon, color, 180
		);
		addFace(
			x + sizeX, y - sizeY, z + sizeZ,
			x + sizeX, y + 0, z + sizeZ,
			x + 0, y + 0, z + sizeZ,
			x + 0, y - sizeY, z + sizeZ,
			icon, color, 180
		);
		addFace(
				x + sizeX, y - sizeY, z + 0,
				x + sizeX, y + 0, z + 0,
				x + sizeX, y + 0, z + sizeZ,
				x + sizeX, y - sizeY, z + sizeZ,
				icon, color, 120
			);
		addFace(
				x + 0, y - sizeY, z + sizeZ,
				x + 0, y + 0, z + sizeZ,
				x + 0, y + 0, z + 0,
				x + 0, y - sizeY, z + 0,
				icon, color, 120
			);
	}
	
	private void addFace(float p1x, float p1y, float p1z,
			float p2x, float p2y, float p2z,
			float p3x, float p3y, float p3z,
			float p4x, float p4y, float p4z,
			IIcon icon, int color, int brightness) {
		int off = vertices.position() * 4;
		vertices.put(new float[] {
				p1x, p1y, p1z, icon.getMinU(), icon.getMaxV(), 0, 0,
				p2x, p2y, p2z, icon.getMinU(), icon.getMinV(), 0, 0,
				p4x, p4y, p4z, icon.getMaxU(), icon.getMaxV(), 0, 0,
				p2x, p2y, p2z, icon.getMinU(), icon.getMinV(), 0, 0,
				p3x, p3y, p3z, icon.getMaxU(), icon.getMinV(), 0, 0,
				p4x, p4y, p4z, icon.getMaxU(), icon.getMaxV(), 0, 0
				});
		buffer.putInt(off + 0 * getStride() + 6 * 4, color);
		buffer.putShort(off + 0 * getStride() + 5 * 4 + 2, (short)brightness);
		buffer.putInt(off + 1 * getStride() + 6 * 4, color);
		buffer.putShort(off + 1 * getStride() + 5 * 4 + 2, (short)brightness);
		buffer.putInt(off + 2 * getStride() + 6 * 4, color);
		buffer.putShort(off + 2 * getStride() + 5 * 4 + 2, (short)brightness);
		buffer.putInt(off + 3 * getStride() + 6 * 4, color);
		buffer.putShort(off + 3 * getStride() + 5 * 4 + 2, (short)brightness);
		buffer.putInt(off + 4 * getStride() + 6 * 4, color);
		buffer.putShort(off + 4 * getStride() + 5 * 4 + 2, (short)brightness);
		buffer.putInt(off + 5 * getStride() + 6 * 4, color);
		buffer.putShort(off + 5 * getStride() + 5 * 4 + 2, (short)brightness);
		
	}
	
	public int getStride() {
		return (3 * 4 + 8 + 4 + 4);
	}
	
}