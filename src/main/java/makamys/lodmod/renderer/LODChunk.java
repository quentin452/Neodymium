package makamys.lodmod.renderer;

import java.util.List;

import makamys.lodmod.LODMod;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagEnd;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.chunk.Chunk;

public class LODChunk {
	
	int x, z;
	public boolean needsChunk = true;
	int lod = 0;
	boolean visible;
	
	SimpleChunkMesh[] simpleMeshes = new SimpleChunkMesh[2];
	ChunkMesh[] chunkMeshes = new ChunkMesh[32];
	
	public boolean[] hidden = new boolean[16];
	
	LODRenderer renderer = LODMod.renderer;
	
	public LODChunk(int x, int z) {
		this.x = x;
		this.z = z;
	}
	
	public LODChunk(NBTTagCompound nbt, List<String> spriteList) {
	    this.x = nbt.getInteger("x");
	    this.z = nbt.getInteger("z");
	    
	    NBTTagCompound chunkMeshesCompound = nbt.getCompoundTag("chunkMeshes");
	    for(Object o : chunkMeshesCompound.func_150296_c()) {
	        String key = (String)o;
	        int keyInt = Integer.parseInt(key);
	        
	        byte[] data = chunkMeshesCompound.getByteArray(key);
	        
	        chunkMeshes[keyInt] = new ChunkMesh(x, keyInt / 2, z, new ChunkMesh.Flags(true, true, true, false), data.length / (2 + 4 * (3 + 2 + 2 + 4)), data, spriteList, keyInt % 2);
	    }
	    
	}
	
	@Override
	public String toString() {
		return "LODChunk(" + x + ", " + z + ")";
	}
	
	public double distSq(Entity entity) {
		return Math.pow(entity.posX - x * 16, 2) + Math.pow(entity.posZ - z * 16, 2);
	}
	
	public void putChunkMeshes(int cy, List<ChunkMesh> newChunkMeshes) {
		for(int i = 0; i < 2; i++) {
		    ChunkMesh newChunkMesh = newChunkMeshes.size() > i ? newChunkMeshes.get(i) : null;
		    if(chunkMeshes[cy * 2 + i] != null) {
			    if(newChunkMesh != null) {
			        newChunkMesh.pass = i;
			    }
			    
			    renderer.setMeshVisible(chunkMeshes[cy * 2 + i], false);
			    chunkMeshes[cy * 2 + i].destroy();
			}
		    chunkMeshes[cy * 2 + i] = newChunkMesh;
		}
		LODMod.renderer.lodChunkChanged(this);
	}
	
	// nice copypasta
	public void putSimpleMeshes(List<SimpleChunkMesh> newSimpleMeshes) {
	    for(int i = 0; i < 2; i++) {
            SimpleChunkMesh newSimpleMesh = newSimpleMeshes.size() > i ? newSimpleMeshes.get(i) : null;
            if(simpleMeshes[i] != null) {
                if(newSimpleMesh != null) {
                    newSimpleMesh.pass = i;
                }
                
                renderer.setMeshVisible(simpleMeshes[i], false);
                simpleMeshes[i].destroy();
            }
            simpleMeshes[i] = newSimpleMesh;
        }
	    LODMod.renderer.lodChunkChanged(this);
	}
	
	public boolean hasChunkMeshes() {
		for(ChunkMesh cm : chunkMeshes) {
			if(cm != null) {
				return true;
			}
		}
		return false;
	}
	
	public void tick(Entity player) {
		double distSq = distSq(player);
		if(distSq < Math.pow((LODMod.renderer.renderRange / 2) * 16, 2)) {
		    renderer.setLOD(this, 2);
		} else if(distSq < Math.pow((LODMod.renderer.renderRange) * 16, 2)) {
		    renderer.setLOD(this, 1);
		} else {
		    renderer.setLOD(this, 0);
		}
	}
	
	public NBTTagCompound saveToNBT() {
	    NBTTagCompound nbt = new NBTTagCompound();
	    nbt.setInteger("x", x);
	    nbt.setInteger("z", z);
	    NBTTagCompound chunkMeshesCompound = new NBTTagCompound();
	    for(int i = 0; i < chunkMeshes.length; i++) {
	        if(chunkMeshes[i] != null) {
	            chunkMeshesCompound.setTag(String.valueOf(i), chunkMeshes[i].nbtData);
	        }
	    }
	    nbt.setTag("chunkMeshes", chunkMeshesCompound);
	    return nbt;
	}
	
	public void destroy() {
	    for(SimpleChunkMesh scm: simpleMeshes) {
	        if(scm != null) {
	            scm.destroy();
	        }
        }
	    for(ChunkMesh cm: chunkMeshes) {
	        if(cm != null) {
	            cm.destroy();
	        }
	    }
	    LODMod.renderer.setVisible(this, false);
	}
	
	public void receiveChunk(Chunk chunk) {
	    putSimpleMeshes(SimpleChunkMesh.generateSimpleMeshes(chunk));
	}
	
	public boolean isFullyVisible() {
	    if(!visible) return false;
	    for(boolean b : hidden) {
	        if(b) {
	            return false;
	        }
	    }
	    return true;
	}
	
	public boolean isSubchunkVisible(int y) {
	    return !hidden[y];
	}
	
}
