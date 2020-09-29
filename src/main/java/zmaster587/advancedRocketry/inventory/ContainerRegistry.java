package zmaster587.advancedRocketry.inventory;

import net.minecraft.client.gui.ScreenManager;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.DistExecutor;
import zmaster587.libVulpes.inventory.ContainerModular;
import zmaster587.libVulpes.inventory.GuiModular;

public class ContainerRegistry {

	public static ContainerType<ContainerOreMappingSatallite> CONTAINER_SATELLITE;
	
	public static void initContainers(RegistryEvent.Register<ContainerType<?>> evt)
	{
		CONTAINER_SATELLITE = IForgeContainerType.create(ContainerOreMappingSatallite::createFromNetworkItem);
    	evt.getRegistry().registerAll(
    			CONTAINER_SATELLITE.setRegistryName("modular_held_item"));
    	
		DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
			ScreenManager.registerFactory(CONTAINER_SATELLITE, GuiOreMappingSatellite::new);
		});
	}
}