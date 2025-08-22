package net.nerdypuzzle.forgemixins;

import net.mcreator.element.ModElementType;
import net.mcreator.plugin.JavaPlugin;
import net.mcreator.plugin.Plugin;
import net.mcreator.plugin.events.PreGeneratorsLoadingEvent;
import net.nerdypuzzle.forgemixins.element.Mixin;
import net.nerdypuzzle.forgemixins.element.MixinGUI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static net.mcreator.element.ModElementTypeLoader.register;

public class Launcher extends JavaPlugin {

	private static final Logger LOG = LogManager.getLogger("Forge mixins");

	public Launcher(Plugin plugin) {
		super(plugin);

		addListener(PreGeneratorsLoadingEvent.class, event -> register(new ModElementType<>("mixin", 'M', MixinGUI::new, Mixin.class)));

		LOG.info("Forge mixins plugin was loaded");
	}

}