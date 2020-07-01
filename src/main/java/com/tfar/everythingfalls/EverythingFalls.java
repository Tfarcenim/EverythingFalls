package com.tfar.everythingfalls;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.server.FMLServerHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static net.minecraft.block.BlockFalling.fallInstantly;

@Mod.EventBusSubscriber
@Mod(modid = EverythingFalls.MODID, name = EverythingFalls.NAME, version = EverythingFalls.VERSION)
public class EverythingFalls {
  public static final String MODID = "everythingfalls";
  public static final String NAME = "Everything Falls";
  public static final String VERSION = "1.0";

  @SubscribeEvent
  public static void instant(TickEvent.ServerTickEvent e) {
    List<Biome> biomes = new ArrayList<>();
    for (Biome biome : ForgeRegistries.BIOMES) biomes.add(biome);
    Biome[] biomesArray = biomes.toArray(new Biome[0]);
    ;
    if (FMLCommonHandler.instance().getMinecraftServerInstance().isServerRunning()) {
      MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
      if (server.getWorld(0).getWorldInfo().getWorldTime() % 5 != 0)return;
      List<EntityPlayerMP> players = server.getPlayerList().getPlayers();
      players.forEach(playerMP ->
      {
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(playerMP.getPosition());
        for (int x = -4; x < 4; x++)
          for (int y = -4; y < 4; y++)
            for (int z = -4; z < 4; z++) {
              pos.setPos(playerMP.getPosition().getX() + x,
                      playerMP.getPosition().getY() + y,
                      playerMP.getPosition().getZ() + z);
              checkFallable(playerMP.getServerWorld(), pos);
            }
      });
    }
  }

  @SubscribeEvent
  public static void fall(BlockEvent.NeighborNotifyEvent e) {
    BlockPos pos = e.getPos();
    WorldServer world = (WorldServer) e.getWorld();
    List<EntityPlayerMP> players = world.getMinecraftServer().getPlayerList().getPlayers();
    boolean nearby = players.stream().anyMatch(playerMP -> playerMP.getPosition().distanceSq(pos) < 1024);
    if (!nearby) return;
    Set<BlockPos> posSet = e.getNotifiedSides().stream().map(pos::offset).collect(Collectors.toSet());
    posSet.add(pos);
    posSet.forEach(pos1 -> checkFallable(world, pos1));
  }

  private static void checkFallable(World worldIn, BlockPos pos) {
    IBlockState state2 = worldIn.getBlockState(pos);
    if (cantFall(state2))
      return;
    if ((worldIn.isAirBlock(pos.down()) || canFallThrough(worldIn.getBlockState(pos.down()))) && pos.getY() >= 0) {
      int i = 32;

      if (worldIn.isAreaLoaded(pos.add(-32, -32, -32), pos.add(32, 32, 32))) {
        if (!worldIn.isRemote) {
          EntityFallingBlock entityfallingblock = new EntityFallingBlock(worldIn, (double) pos.getX() + 0.5D, (double) pos.getY(), (double) pos.getZ() + 0.5D, worldIn.getBlockState(pos));
          worldIn.spawnEntity(entityfallingblock);
        }
      }
    }
  }

  public static boolean cantFall(IBlockState state) {
    Block block = state.getBlock();
    Material material = state.getMaterial();
    return block == Blocks.FIRE || material == Material.AIR || material == Material.WATER || material == Material.LAVA
            || block == Blocks.BEDROCK;
  }

  public static boolean canFallThrough(IBlockState state) {
    Block block = state.getBlock();
    Material material = state.getMaterial();
    return block == Blocks.FIRE || material == Material.AIR || material == Material.WATER || material == Material.LAVA;
  }
}
