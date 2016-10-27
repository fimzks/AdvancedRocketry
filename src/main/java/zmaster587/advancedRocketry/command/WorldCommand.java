package zmaster587.advancedRocketry.command;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import zmaster587.advancedRocketry.api.Configuration;
import zmaster587.advancedRocketry.api.DataStorage.DataType;
import zmaster587.advancedRocketry.api.IAtmosphere;
import zmaster587.advancedRocketry.api.dimension.IDimensionProperties;
import zmaster587.advancedRocketry.api.dimension.solar.StellarBody;
import zmaster587.advancedRocketry.api.stations.ISpaceObject;
import zmaster587.advancedRocketry.atmosphere.AtmosphereHandler;
import zmaster587.advancedRocketry.atmosphere.AtmosphereType;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.item.ItemData;
import zmaster587.advancedRocketry.item.ItemMultiData;
import zmaster587.advancedRocketry.network.PacketDimInfo;
import zmaster587.advancedRocketry.stations.SpaceObject;
import zmaster587.advancedRocketry.stations.SpaceObjectManager;
import zmaster587.advancedRocketry.world.biome.BiomeGenAlienForest;
import zmaster587.advancedRocketry.world.biome.BiomeGenDeepSwamp;
import zmaster587.advancedRocketry.world.util.TeleporterNoPortal;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.util.HashedBlockPosition;
import zmaster587.libVulpes.util.Vector3F;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class WorldCommand implements ICommand {



	private List aliases;
	public WorldCommand() {
		aliases = new ArrayList<String>();
		aliases.add("advancedRocketry");
		aliases.add("advRocketry");
	}

	@Override
	public String getCommandName() {
		return "advancedRocketry";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "advancedRocketry help";
	}

	@Override
	public List getCommandAliases() {
		return aliases;
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender,
			String[] string) throws CommandException {

		//advRocketry planet set <var value>
		int opLevel = 2;

		if(string.length >= 1 &&  string[0].equalsIgnoreCase("filldata")) {
			ItemStack stack;
			if(sender.getCommandSenderEntity() != null ) {
				stack = ((EntityPlayer)sender.getCommandSenderEntity()).getHeldItem(EnumHand.MAIN_HAND);
				if(stack != null && stack.getItem() instanceof ItemMultiData) {
					ItemMultiData item = (ItemMultiData) stack.getItem();
					for(DataType type : DataType.values())
						item.setData(stack, 2000, type);
					sender.addChatMessage(new TextComponentString("Data filled!"));
				}
				else
					sender.addChatMessage(new TextComponentString("Not Holding data item"));
			}
			else
				sender.addChatMessage(new TextComponentString("Ghosts don't have items!"));
			return;
		}
		
		if(string.length > 1) {
			if(string[0].equalsIgnoreCase("goto") && (string.length == 2 || string.length == 3)) {
				EntityPlayer player = (EntityPlayer) sender.getCommandSenderEntity();
				if(player != null) {
					try {
						int dim;

						if(string.length == 2) {
							dim = Integer.parseInt(string[1]);
							if(net.minecraftforge.common.DimensionManager.isDimensionRegistered(dim))
								player.getServer().getPlayerList().transferPlayerToDimension((EntityPlayerMP) player,  dim , new TeleporterNoPortal((WorldServer) player.worldObj));
							else
								sender.addChatMessage(new TextComponentString("Dimension does not exist"));
						}
						else if(string[1].equalsIgnoreCase("station")) {
							dim = Configuration.spaceDimId;
							int stationId = Integer.parseInt(string[2]);
							ISpaceObject object = SpaceObjectManager.getSpaceManager().getSpaceStation(stationId);

							if(object != null) {
								if(player.worldObj.provider.getDimension() != Configuration.spaceDimId)
									player.getServer().getPlayerList().transferPlayerToDimension((EntityPlayerMP) player,  dim , new TeleporterNoPortal((WorldServer) player.worldObj));
								HashedBlockPosition vec = object.getSpawnLocation();
								player.setPositionAndUpdate(vec.x, vec.y, vec.z);
							}
							else {
								sender.addChatMessage(new TextComponentString("Station " + stationId + " does not exist!"));
							}
						}


					} catch(NumberFormatException e) {
						sender.addChatMessage(new TextComponentString(string[0] + " <dimId>"));
						sender.addChatMessage(new TextComponentString(string[0] + "station <station ID>"));
					}
				}					
				else 
					sender.addChatMessage(new TextComponentString("Must be a player to use this command"));
			}
			else if(string[0].equalsIgnoreCase("fetch") && string.length == 2) {
				EntityPlayer me = (EntityPlayer) sender.getCommandSenderEntity();
				EntityPlayer player = null;

				for(World world : me.getServer().worldServers) {
					player = world.getPlayerEntityByName(string[1]);
					if(player != null)
						break;
				}



				System.out.println(string[1] + "   " + sender.getCommandSenderEntity());

				if(player == null) {
					sender.addChatMessage(new TextComponentString("Invalid player name: " + string[1]));
				}
				else {
					player.getServer().getPlayerList().transferPlayerToDimension((EntityPlayerMP) player,  me.worldObj.provider.getDimension() , new TeleporterNoPortal(me.getServer().worldServerForDimension(me.worldObj.provider.getDimension())));
					player.setPosition(me.posX, me.posY, me.posZ);
				}
			}
			else if(string[0].equalsIgnoreCase("planet")) {

				int dimId;
				if(string[1].equalsIgnoreCase("reset")) {
					if(string.length == 3) {
						try {
							dimId = Integer.parseInt(string[2]);
							DimensionManager.getInstance().getDimensionProperties(dimId).resetProperties();
							PacketHandler.sendToAll(new PacketDimInfo(dimId, DimensionManager.getInstance().getDimensionProperties(dimId)));
						} catch (NumberFormatException e) {
							sender.addChatMessage(new TextComponentString("Invalid dimId"));
						}
					}
					else if(string.length == 2) {
						if(DimensionManager.getInstance().isDimensionCreated((dimId = sender.getEntityWorld().provider.getDimension()))) {
							DimensionManager.getInstance().getDimensionProperties(dimId).resetProperties();
							PacketHandler.sendToAll(new PacketDimInfo(dimId, DimensionManager.getInstance().getDimensionProperties(dimId)));
						}
					}
				}
				else if(string[1].equalsIgnoreCase("list")) { //Lists dimensions

					sender.addChatMessage(new TextComponentString("Dimensions:"));
					for(int i : DimensionManager.getInstance().getregisteredDimensions()) {
						sender.addChatMessage(new TextComponentString("DIM" + i + ":  " + DimensionManager.getInstance().getDimensionProperties(i).getName())); 
					}
				}
				else if(string[1].equalsIgnoreCase("delete")) {
					// advRocketry planet delete <name>
					if(string.length == 3) {
						int deletedDimId;
						try {
							deletedDimId = Integer.parseInt(string[2]);

							if(DimensionManager.getInstance().isDimensionCreated(deletedDimId)) {

								if(net.minecraftforge.common.DimensionManager.getWorld(deletedDimId) == null || net.minecraftforge.common.DimensionManager.getWorld(deletedDimId).playerEntities.isEmpty()) {
									DimensionManager.getInstance().deleteDimension(deletedDimId);
									PacketHandler.sendToAll(new PacketDimInfo(deletedDimId, null));
									sender.addChatMessage(new TextComponentString("Deleted!"));
								}
								else {
									//If the world still has players abort and list players
									sender.addChatMessage(new TextComponentString("World still has players:"));

									for(EntityPlayer player : (List<EntityPlayer>)net.minecraftforge.common.DimensionManager.getWorld(deletedDimId).playerEntities) {
										sender.addChatMessage(player.getDisplayName());
									}

								}


							} else {
								sender.addChatMessage(new TextComponentString("Dimension does not exist"));
							}

						} catch(NumberFormatException exception) {
							sender.addChatMessage(new TextComponentString("Invalid Argument"));
						}
					}
					else {
						sender.addChatMessage(new TextComponentString(string[0] + " " + string[1] + " " + string[2] + " <dimid>"));
					}
				}
				/*
				 * Attempt to generate a planet
				 */
				else if(string[1].equalsIgnoreCase("generate")) {

					int gasOffset = 0;
					boolean gassy = false;
					int starId = 0;

					if(string.length > 2 ) {
						try {
							starId = Integer.parseInt(string[2]);
							gasOffset++;
							if(DimensionManager.getInstance().getStar(starId) == null) {
								sender.addChatMessage(new TextComponentString("Invalid star ID"));
								sender.addChatMessage(new TextComponentString(string[0] + " " + string[1] + "[starId] [gas] <name> <atmosphereRandomness> <distanceRandomness> <gravityRandomness>"));

								return;
							}

						} catch(NumberFormatException e) {

						}
					}

					if(string.length > 2 + gasOffset && string[2 + gasOffset].equalsIgnoreCase("gas")) {
						gasOffset++;
						gassy = true;
					}

					try {
						//Advancedrocketry planet generate <name> <atmosphereRandomness> <distanceRandomness> <gravityRandomness>
						if(string.length == 6 + gasOffset) {
							if(!gassy)
								DimensionManager.getInstance().generateRandom(starId,string[2 + gasOffset], Integer.parseInt(string[3 + gasOffset]), Integer.parseInt(string[4 + gasOffset]), Integer.parseInt(string[5 + gasOffset]));
							else
								DimensionManager.getInstance().generateRandomGasGiant(starId,string[2 + gasOffset], Integer.parseInt(string[3 + gasOffset]), Integer.parseInt(string[4 + gasOffset]), Integer.parseInt(string[5 + gasOffset]),0,0,0);

							sender.addChatMessage(new TextComponentString("Dimension Generated!"));
						}
						else if(string.length == 9  + gasOffset) {
							if(!gassy)
								DimensionManager.getInstance().generateRandom(starId,string[2 + gasOffset] ,Integer.parseInt(string[3 + gasOffset]), Integer.parseInt(string[4 + gasOffset]), Integer.parseInt(string[5 + gasOffset]),Integer.parseInt(string[6 + gasOffset]), Integer.parseInt(string[7 + gasOffset]), Integer.parseInt(string[8 + gasOffset]));
							else
								DimensionManager.getInstance().generateRandomGasGiant(starId, string[2 + gasOffset] ,Integer.parseInt(string[3 + gasOffset]), Integer.parseInt(string[4 + gasOffset]), Integer.parseInt(string[5 + gasOffset]),Integer.parseInt(string[6 + gasOffset]), Integer.parseInt(string[7 + gasOffset]), Integer.parseInt(string[8 + gasOffset]));

							sender.addChatMessage(new TextComponentString("Dimension: " + string[2 + gasOffset] + " Generated!"));
						}
						else {
							sender.addChatMessage(new TextComponentString(string[0] + " " + string[1] + " [starId] [gas] <name> <atmosphereRandomness> <distanceRandomness> <gravityRandomness>"));
							sender.addChatMessage(new TextComponentString(""));
							sender.addChatMessage(new TextComponentString(string[0] + " " + string[1] + " [starId] [gas] <name> <atmosphere base value> <distance base value> <gravity base value> <atmosphereRandomness> <distanceRandomness> <gravityRandomness>"));
						}
					} catch(NumberFormatException e) {
						sender.addChatMessage(new TextComponentString(string[0] + " " + string[1] + " [starId] [gas] <name> <atmosphereRandomness> <distanceRandomness> <gravityRandomness>"));
						sender.addChatMessage(new TextComponentString(""));
						sender.addChatMessage(new TextComponentString(string[0] + " " + string[1] + " [starId] [gas] <name> <atmosphere base value> <distance base value> <gravity base value> <atmosphereRandomness> <distanceRandomness> <gravityRandomness>"));
					}
				}
				//Make sure player is in Dimension we have control over
				else if( DimensionManager.getInstance().isDimensionCreated((dimId = sender.getEntityWorld().provider.getDimension())) ) {

					if(string[1].equalsIgnoreCase("set") && string.length > 2) {

						DimensionProperties properties = DimensionManager.getInstance().getDimensionProperties(dimId);


						try {
							if(string[2].equalsIgnoreCase("atmosphereDensity")) {
								properties.setAtmosphereDensityDirect(Integer.parseUnsignedInt(string[3]));
								PacketHandler.sendToAll(new PacketDimInfo(dimId, properties));
							}
							else {

								Field field = properties.getClass().getDeclaredField(string[2]);

								if(field.getType().isArray()) {

									if(Float.TYPE == field.getType().getComponentType()) {
										float var[] = (float[])field.get(properties);

										if(string.length - 3 == var.length) {

											//Make sure we catch if some invalid arg is entered
											for(int i = 0; i < var.length; i++) {
												var[i] = Float.parseFloat(string[3+i]);
											}

											field.set(properties, var);

										}
									}

									if(Integer.TYPE == field.getType().getComponentType()) {
										int var[] = (int[])field.get(properties);

										if(string.length - 3 == var.length) {

											//Make sure we catch if some invalid arg is entered

											for(int i = 0; i < var.length; i++) {
												var[i] = Integer.parseInt(string[3+i]);
											}

											field.set(properties, var);

										}
									}
								}
								else {
									if(Integer.TYPE == field.getType() )
										field.set(properties, Integer.parseInt(string[3]));
									else if(Float.TYPE == field.getType())
										field.set(properties, Float.parseFloat(string[3]));
									else
										field.set(properties, string[3]);
								}

								PacketHandler.sendToAll(new PacketDimInfo(dimId, properties));
								return;
							}
						} catch (NumberFormatException e) {

							sender.addChatMessage(new TextComponentString("Invalid Argument for parameter: " + string[2]));
						} catch (Exception e) {

							e.printStackTrace();
						}
					}
					else if(string[1].equalsIgnoreCase("get") && string.length == 3) {
						DimensionProperties properties = DimensionManager.getInstance().getDimensionProperties(dimId);
						if(string[2].equalsIgnoreCase("atmosphereDensity")) {
							sender.addChatMessage(new TextComponentString(Integer.toString(properties.getAtmosphereDensity())));
						} 
						else {
							try {
								Field field = properties.getClass().getDeclaredField(string[2]);

								sender.addChatMessage(new TextComponentString(field.get(properties).toString()));

							} catch (Exception e) {

								e.printStackTrace();
							}
						}
					}
				}
			} //string[0] = planet
			else if(string[0].equals("star")) {
				if(string.length > 1) {
					if(string[1].equalsIgnoreCase("list")) {
						for(StellarBody star : DimensionManager.getInstance().getStars())
							sender.addChatMessage(new TextComponentString(String.format("Star ID: %d   Name: %s  Num Planets: %d", star.getId(), star.getName(), star.getNumPlanets())));
					}
					else if(string[1].equalsIgnoreCase("help")) {
						printStarHelp(sender);
					}
				}
				if(string.length > 3) {
					if(string[1].equalsIgnoreCase("get")) {
						try {
							int id = Integer.parseInt(string[3]);
							StellarBody star =  DimensionManager.getInstance().getStar(id);
							if(star == null)
								sender.addChatMessage(new TextComponentString("Error: " + string[3] + " is not a valid star ID"));
							else {
								if(string[2].equalsIgnoreCase("temp")) {
									sender.addChatMessage(new TextComponentString("Temp: " + star.getTemperature()));
								}
								else if(string[2].equalsIgnoreCase("planets")) {
									sender.addChatMessage(new TextComponentString("Planets orbiting the star:"));
									for(IDimensionProperties planets : star.getPlanets()) {
										sender.addChatMessage(new TextComponentString("ID: " + planets.getId() + " : " + planets.getName()));
									}
								}
								else if(string[2].equalsIgnoreCase("pos")) {
									sender.addChatMessage(new TextComponentString("Pos: " + star.getPosX() + "," + star.getPosZ()));
								}
							}// end star existance validation
						} catch (NumberFormatException e) {
							sender.addChatMessage(new TextComponentString("Error: " + string[3] + " is not a valid star ID"));
						}
					} //get
				} if(string.length > 4) {
					if(string[1].equalsIgnoreCase("set")) {
						try {
							int id = Integer.parseInt(string[3]);
							StellarBody star =  DimensionManager.getInstance().getStar(id);
							if(star == null)
								sender.addChatMessage(new TextComponentString("Error: " + string[3] + " is not a valid star ID"));
							else {
								if(string[2].equalsIgnoreCase("temp")) {
									try {
										star.setTemperature(Integer.parseInt(string[4]));
										sender.addChatMessage(new TextComponentString("Temp set to " + star.getTemperature()));
									} catch(NumberFormatException e) {
										sender.addChatMessage(new TextComponentString("star set temp <starId> <temp>"));
									}
								} else if(string.length > 5 && string[2].equalsIgnoreCase("pos")) {
									try {
										int x= Integer.parseInt(string[4]);
										int z = Integer.parseInt(string[5]);
										star.setPosX(x);
										star.setPosZ(z);
										sender.addChatMessage(new TextComponentString("Position set to " + x + "," + z));
									} catch(NumberFormatException e) {
										sender.addChatMessage(new TextComponentString("star set pos <starId> <x> <y>"));
									}
								}
							}// end star existance validation
						} catch (NumberFormatException e) {
							sender.addChatMessage(new TextComponentString("Error: " + string[3] + " is not a valid star ID"));
						}
					}
				}
				if(string.length > 5) {
					if(string[1].equalsIgnoreCase("generate")) {
						try {
							String name = string[2];
							int temp = Integer.parseInt(string[3]);
							int x = Integer.parseInt(string[4]);
							int z = Integer.parseInt(string[5]);
							StellarBody star = new StellarBody();
							star.setTemperature(temp);
							star.setPosX(x);
							star.setPosZ(z);
							star.setName(name);
							star.setId(DimensionManager.getInstance().getNextFreeStarId());
							if(star.getId() != -1) {
								DimensionManager.getInstance().addStar(star);
								sender.addChatMessage(new TextComponentString("star Added!"));
							}
							else
								sender.addChatMessage(new TextComponentString("Why can't I hold all these stars! (either you have an insane number of stars or something really broke!)"));

						} catch(NumberFormatException e) {
							sender.addChatMessage(new TextComponentString("star generate <name> <temp> <x> <y>"));
						}
					}
				}
			} //string[0] = star
		} // len > 2

	}

	private void printStarHelp(ICommandSender sender) {
		sender.addChatMessage(new TextComponentString("star list"));
		sender.addChatMessage(new TextComponentString("star get temp <star id>"));
		sender.addChatMessage(new TextComponentString("star get planets <star id>"));
		sender.addChatMessage(new TextComponentString("star get pos <star id>"));
		sender.addChatMessage(new TextComponentString("star set temp <star id> <temperature>"));
		sender.addChatMessage(new TextComponentString("star set pos <star id> <x> <y>"));
		sender.addChatMessage(new TextComponentString("star generate <name> <temp> <x> <y>"));
	}

	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
		return sender.getCommandSenderEntity() != null && sender.canCommandSenderUseCommand(2, getCommandName());

	}

	@Override
	public List<String> getTabCompletionOptions(MinecraftServer server,
			ICommandSender sender, String[] string, BlockPos pos) {
		ArrayList<String> list = new ArrayList<String>();

		if(string.length == 1) {
			list.add("planet");
			list.add("goto");
			list.add("fetch");
			list.add("star");
			list.add("filldata");
		} else if(string.length == 2) {
			ArrayList<String> list2 = new ArrayList<String>();
			list2.add("get");
			list2.add("set");
			list2.add("list");
			list2.add("generate");
			if(string[0].equalsIgnoreCase("planet")) {
				list2.add("reset");
				list2.add("new");
				list2.add("delete");


				for(String str : list2) {
					if(str.startsWith(string[1]))
						list.add(str);
				}
			}
		} else if(( string[1].equalsIgnoreCase("get") || string[1].equalsIgnoreCase("set")) && string[0].equalsIgnoreCase("planet") && string.length == 3) {
			for(Field field : DimensionProperties.class.getFields()) {
				if(field.getName().startsWith(string[2]))
					list.add(field.getName());

			}
			list.add("atmosphereDensity");
		}

		return list;
	}

	@Override
	public boolean isUsernameIndex(String[] string, int number) {
		return number == 1 && string[0].equalsIgnoreCase("fetch");
	}

	@Override
	public int compareTo(ICommand arg0) {
		return this.getCommandName().compareTo(arg0.getCommandName());
	}
}
