package com.seibel.distanthorizons.common.wrappers.gui;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

// Logger (for debug stuff)

import com.seibel.distanthorizons.api.enums.config.DisallowSelectingViaConfigGui;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.config.ConfigBase;
import com.seibel.distanthorizons.core.config.types.*;
import com.seibel.distanthorizons.common.wrappers.gui.updater.ChangelogScreen;

// Uses https://github.com/TheElectronWill/night-config for toml (only for Fabric since Forge already includes this)

// Gets info from our own mod

// Minecraft imports

import com.seibel.distanthorizons.core.config.types.enums.EConfigCommentTextPosition;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.jar.updater.SelfUpdater;
import com.seibel.distanthorizons.core.logging.ConfigBasedSpamLogger;
import com.seibel.distanthorizons.core.logging.SpamReducedLogger;
import com.seibel.distanthorizons.core.render.renderer.LodRenderer;
import com.seibel.distanthorizons.core.util.AnnotationUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.config.IConfigGui;
import com.seibel.distanthorizons.core.wrapperInterfaces.config.ILangWrapper;
import com.seibel.distanthorizons.coreapi.ModInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;


#if MC_VER < MC_1_20_1
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
#else
import net.minecraft.client.gui.GuiGraphics;
#endif

#if MC_VER >= MC_1_17_1
import net.minecraft.client.gui.narration.NarratableEntry;
#endif

import static com.seibel.distanthorizons.common.wrappers.gui.GuiHelper.*;
import static com.seibel.distanthorizons.common.wrappers.gui.GuiHelper.Translatable;


/*
 * Based upon TinyConfig but is highly modified
 * https://github.com/Minenash/TinyConfig
 *
 * Note: floats don't work with this system, use doubles.
 *
 * @author coolGi
 * @author Motschen
 * @author James Seibel
 * @version 5-21-2022
 */
@SuppressWarnings("unchecked")
public class ClassicConfigGUI
{
	private static final Logger LOGGER = LogManager.getLogger();
	public static final SpamReducedLogger SPAM_LOGGER = new SpamReducedLogger(4);
	
	public static final ConfigCoreInterface CONFIG_CORE_INTERFACE = new ConfigCoreInterface();
	
	
	
	//==============//
	// Initializers //
	//==============//
	
	// Some regexes to check if an input is valid
	private static final Pattern INTEGER_ONLY_REGEX = Pattern.compile("(-?[0-9]*)");
	private static final Pattern DECIMAL_ONLY_REGEX = Pattern.compile("-?([\\d]+\\.?[\\d]*|[\\d]*\\.?[\\d]+|\\.)");
	
	private static class ConfigScreenConfigs
	{
		// This contains all the configs for the configs
		public static final int SPACE_FROM_RIGHT_SCREEN = 10;
		public static final int SPACE_BETWEEN_TEXT_AND_OPTION_FIELD = 8;
		public static final int BUTTON_WIDTH_SPACING = 5;
		public static final int RESET_BUTTON_WIDTH = 60;
		public static final int RESET_BUTTON_HEIGHT = 20;
		public static final int OPTION_FIELD_WIDTH = 150;
		public static final int OPTION_FIELD_HEIGHT = 20;
		public static final int CATEGORY_BUTTON_WIDTH = 200;
		public static final int CATEGORY_BUTTON_HEIGHT = 20;
		
	}
	
	// TODO
	/** The terribly coded old stuff */
	public static class EntryInfo
	{
		Object widget;
		Map.Entry<EditBox, Component> error;
		String tempValue;
		int index;
	}
	
	
	
	//==============//
	// GUI handling //
	//==============//
	
	/** if you want to get this config gui's screen call this */
	public static Screen getScreen(ConfigBase configBase, Screen parent, String category)
	{ return new ConfigScreen(configBase, parent, category); }
	
	/** Pain */
	private static class ConfigScreen extends DhScreen
	{
		private static final ILangWrapper LANG_WRAPPER = SingletonInjector.INSTANCE.get(ILangWrapper.class);
		
		private static final String TRANSLATION_PREFIX = ModInfo.ID + ".config.";
		
		
		private final ConfigBase configBase;
		
		private final Screen parent;
		private final String category;
		private ConfigListWidget configListWidget;
		private boolean reload = false;
		
		private Button doneButton;
		
		
		
		//=============//
		// constructor //
		//=============//
		
		protected ConfigScreen(ConfigBase configBase, Screen parent, String category)
		{
			super(Translatable(
					LANG_WRAPPER.langExists(ModInfo.ID + ".config" + (category.isEmpty() ? "." + category : "") + ".title") ?
							ModInfo.ID + ".config.title" :
							ModInfo.ID + ".config" + (category.isEmpty() ? "" : "." + category) + ".title")
			);
			this.configBase = configBase;
			this.parent = parent;
			this.category = category;
		}
		
		
		@Override
		public void tick() { super.tick(); }
		
		
		
		//==================//
		// menu UI creation //
		//==================//
		
		@Override
		protected void init()
		{
			super.init();
			if (!this.reload)
			{
				ConfigBase.INSTANCE.configFileHandler.loadFromFile();
			}
			
			// Changelog button
			if (Config.Client.Advanced.AutoUpdater.enableAutoUpdater.get()
				// we only have changelogs for stable builds		
				&& !ModInfo.IS_DEV_BUILD)
			{
				this.addBtn(new TexturedButtonWidget(
						// Where the button is on the screen
						this.width - 28, this.height - 28,
						// Width and height of the button
						20, 20,
						// texture UV Offset
						0, 0,
						// Some texture stuff
						0, 
						#if MC_VER < MC_1_21_1
						new ResourceLocation(ModInfo.ID, "textures/gui/changelog.png"),
						#else
						ResourceLocation.fromNamespaceAndPath(ModInfo.ID, "textures/gui/changelog.png"),
						#endif
						20, 20,
						// Create the button and tell it where to go
						(buttonWidget) -> {
							ChangelogScreen changelogScreen = new ChangelogScreen(this);
							if (changelogScreen.usable)
							{
								Objects.requireNonNull(this.minecraft).setScreen(changelogScreen);
							}
							else
							{
								LOGGER.warn("Changelog was not able to open");
							}
						},
						// Add a title to the button
						Translatable(ModInfo.ID + ".updater.title")
				));
			}
			
			
			// back button
			this.addBtn(MakeBtn(Translatable("distanthorizons.general.back"),
					(this.width / 2) - 154, this.height - 28,
					ConfigScreenConfigs.OPTION_FIELD_WIDTH, ConfigScreenConfigs.OPTION_FIELD_HEIGHT,
					(button) -> 
					{
						ConfigBase.INSTANCE.configFileHandler.loadFromFile();
						Objects.requireNonNull(this.minecraft).setScreen(this.parent);
					}));
			
			// done/close button
			this.doneButton = this.addBtn(
					MakeBtn(Translatable("distanthorizons.general.done"),
							(this.width / 2) + 4, this.height - 28,
							ConfigScreenConfigs.OPTION_FIELD_WIDTH, ConfigScreenConfigs.OPTION_FIELD_HEIGHT, 
					(button) -> 
					{
						ConfigBase.INSTANCE.configFileHandler.saveToFile();
						Objects.requireNonNull(this.minecraft).setScreen(this.parent);
					}));
			
			this.configListWidget = new ConfigListWidget(this.minecraft, this.width * 2, this.height, 32, 32, 25);
			
			#if MC_VER < MC_1_20_6 // no background is rendered in MC 1.20.6+
			if (this.minecraft != null && this.minecraft.level != null)
			{
				this.configListWidget.setRenderBackground(false);
			}
			#endif
			
			this.addWidget(this.configListWidget);
			
			for (AbstractConfigType info : ConfigBase.INSTANCE.entries)
			{
				try
				{
					if (info.getCategory().matches(this.category) 
						&& info.getAppearance().showInGui)
					{
						this.addMenuItem(info);
					}
				}
				catch (Exception e)
				{
					String message = "ERROR: Failed to show [" + info.getNameWCategory() + "], error: ["+e.getMessage()+"]";
					if (info.get() != null)
					{
						message += " with the value [" + info.get() + "] with type [" + info.getType() + "]";
					}
					
					LOGGER.error(message, e);
				}
			}
			
			CONFIG_CORE_INTERFACE.onScreenChangeListenerList.forEach((listener) -> listener.run());
		}
		private void addMenuItem(AbstractConfigType configType)
		{
			trySetupConfigEntry(configType);
			
			if (this.tryCreateInputField(configType)) return;
			if (this.tryCreateCategoryButton(configType)) return;
			if (this.tryCreateButton(configType)) return;
			if (this.tryCreateComment(configType)) return;
			if (this.tryCreateSpacer(configType)) return;
			if (this.tryCreateLinkedEntry(configType)) return;
			
			LOGGER.warn("Config [" + configType.getNameWCategory() + "] failed to show. Please try something like changing its type.");
		}
		
		private static void trySetupConfigEntry(AbstractConfigType configType)
		{
			configType.guiValue = new EntryInfo();
			Class<?> fieldClass = configType.getType();
			
			if (configType instanceof ConfigEntry)
			{
				ConfigEntry configEntry = (ConfigEntry) configType;
				
				if (fieldClass == Integer.class)
				{
					// For int
					setupEntryInfoTextField(configEntry, Integer::parseInt, INTEGER_ONLY_REGEX, true);
				}
				else if (fieldClass == Double.class)
				{
					// For double
					setupEntryInfoTextField(configEntry, Double::parseDouble, DECIMAL_ONLY_REGEX, false);
				}
				else if (fieldClass == String.class || fieldClass == List.class)
				{
					// For string or list
					setupEntryInfoTextField(configEntry, String::length, null, true);
				}
				else if (fieldClass == Boolean.class)
				{
					// For boolean
					Function<Object, Component> func = value -> Translatable("distanthorizons.general."+((Boolean) value ? "true" : "false")).withStyle((Boolean) value ? ChatFormatting.GREEN : ChatFormatting.RED);
					
					((EntryInfo) configEntry.guiValue).widget =
							new AbstractMap.SimpleEntry<Button.OnPress, Function<Object, Component>>(
									(button) ->
									{
										button.active = !configEntry.apiIsOverriding();
										
										configEntry.uiSetWithoutSaving(!(Boolean) configEntry.get());
										button.setMessage(func.apply(configEntry.get()));
									}, func);
				}
				else if (fieldClass.isEnum())
				{
					// For enum
					List<?> values = Arrays.asList(configEntry.getType().getEnumConstants());
					Function<Object, Component> func = (value) -> Translatable(TRANSLATION_PREFIX + "enum." + fieldClass.getSimpleName() + "." + configEntry.get().toString());
					((EntryInfo) configEntry.guiValue).widget = new AbstractMap.SimpleEntry<Button.OnPress, Function<Object, Component>>((button) ->
					{
						// get the currently selected enum and enum index
						int startingIndex = values.indexOf(configEntry.get());
						Enum<?> enumValue = (Enum<?>) values.get(startingIndex);
						
						// search for the next enum that is selectable
						int index = startingIndex + 1;
						index = (index >= values.size()) ? 0 : index;
						while (index != startingIndex)
						{
							enumValue = (Enum<?>) values.get(index);
							if (!AnnotationUtil.doesEnumHaveAnnotation(enumValue, DisallowSelectingViaConfigGui.class))
							{
								// this enum shouldn't be selectable via the UI,
								// skip it
								break;
							}
							
							index++;
							index = (index >= values.size()) ? 0 : index;
						}
						
						if (index == startingIndex)
						{
							// none of the enums should be selectable, this is a programmer error
							enumValue = (Enum<?>) values.get(startingIndex);
							LOGGER.warn("Enum [" + enumValue.getClass() + "] doesn't contain any values that should be selectable via the UI, sticking to the currently selected value [" + enumValue + "].");
						}
						
						
						((ConfigEntry<Enum<?>>) configEntry).uiSetWithoutSaving(enumValue);
						
						if (configEntry.getApiValue() != null)
						{
							button.active = false;
						}
						else
						{
							button.active = true;
						}
						
						button.setMessage(func.apply(configEntry.get()));
					}, func);
				}
			}
			
		}
		private static void setupEntryInfoTextField(AbstractConfigType info, Function<String, Number> func, Pattern pattern, boolean cast)
		{
			((EntryInfo) info.guiValue).widget = (BiFunction<EditBox, Button, Predicate<String>>) (editBox, button) -> stringValue ->
			{
				boolean isNumber = (pattern != null);
				
				stringValue = stringValue.trim();
				if (!(stringValue.isEmpty() || !isNumber || pattern.matcher(stringValue).matches()))
				{
					return false;
				}
				
				
				Number value = info.typeIsFloatingPointNumber() ? 0.0 : 0; // different default values are needed so implicit casting works correctly (if not done casting from 0 (an int) to a double will cause an exception)
				((EntryInfo) info.guiValue).error = null;
				if (isNumber && !stringValue.isEmpty() && !stringValue.equals("-") && !stringValue.equals("."))
				{
					try
					{
						value = func.apply(stringValue);
					}
					catch (Exception e)
					{
						value = null;
					}
					
					byte isValid = ((ConfigEntry) info).isValid(value);
					switch (isValid)
					{
						case 0:
							((EntryInfo) info.guiValue).error = null;
							break;
						case -1:
							((EntryInfo) info.guiValue).error = new AbstractMap.SimpleEntry<>(editBox, TextOrTranslatable("§cMinimum length is " + ((ConfigEntry) info).getMin()));
							break;
						case 1:
							((EntryInfo) info.guiValue).error = new AbstractMap.SimpleEntry<>(editBox, TextOrTranslatable("§cMaximum length is " + ((ConfigEntry) info).getMax()));
							break;
						case 2:
							((EntryInfo) info.guiValue).error = new AbstractMap.SimpleEntry<>(editBox, TextOrTranslatable("§cValue is invalid"));
							break;
					}
				}
				
				((EntryInfo) info.guiValue).tempValue = stringValue;
				editBox.setTextColor(((ConfigEntry) info).isValid(value) == 0 ? 0xFFFFFFFF : 0xFFFF7777); // white and red
				
				
				if (info.getType() == String.class
						|| info.getType() == List.class)
				{
					((ConfigEntry) info).uiSetWithoutSaving(stringValue);
				}
				else if (((ConfigEntry) info).isValid(value) == 0)
				{
					if (!cast)
					{
						((ConfigEntry) info).uiSetWithoutSaving(value);
					}
					else
					{
						((ConfigEntry) info).uiSetWithoutSaving(value != null ? value.intValue() : 0);
					}
				}
				
				return true;
			};
		}
		
		private boolean tryCreateInputField(AbstractConfigType configType)
		{
			if (configType instanceof ConfigEntry)
			{
				ConfigEntry configEntry = (ConfigEntry) configType;
				
				
				//==============//
				// reset button //
				//==============//
				
				Button.OnPress btnAction = (button) ->
				{
					configEntry.uiSetWithoutSaving(configEntry.getDefaultValue());
					((EntryInfo) configEntry.guiValue).index = 0;
					this.reload = true;
					Objects.requireNonNull(this.minecraft).setScreen(this);
				};
				
				int resetButtonPosX = this.width
						- ConfigScreenConfigs.RESET_BUTTON_WIDTH
						- ConfigScreenConfigs.SPACE_FROM_RIGHT_SCREEN;
				int resetButtonPosZ = 0;
				
				Button resetButton = MakeBtn(
						Translatable("distanthorizons.general.reset").withStyle(ChatFormatting.RED),
						resetButtonPosX, resetButtonPosZ,
						ConfigScreenConfigs.RESET_BUTTON_WIDTH, ConfigScreenConfigs.RESET_BUTTON_HEIGHT,
						btnAction);
				
				if (configEntry.apiIsOverriding())
				{
					resetButton.active = false;
					resetButton.setMessage(Translatable("distanthorizons.general.apiOverride").withStyle(ChatFormatting.DARK_GRAY));
				}
				else
				{
					resetButton.active = true;
				}
				
				
				
				//==============//
				// option field //
				//==============//
				
				Component textComponent = this.GetTranslatableTextComponentForConfig(configEntry);
				
				int optionFieldPosX = this.width
						- ConfigScreenConfigs.SPACE_FROM_RIGHT_SCREEN
						- ConfigScreenConfigs.RESET_BUTTON_WIDTH
						- ConfigScreenConfigs.BUTTON_WIDTH_SPACING
						- ConfigScreenConfigs.OPTION_FIELD_WIDTH;
				int optionFieldPosZ = 0;
				
				if (((EntryInfo) configEntry.guiValue).widget instanceof Map.Entry)
				{
					// enum/multi option input button
					
					Map.Entry<Button.OnPress, Function<Object, Component>> widget = (Map.Entry<Button.OnPress, Function<Object, Component>>) ((EntryInfo) configEntry.guiValue).widget;
					if (configEntry.getType().isEnum())
					{
						widget.setValue((value) -> Translatable(TRANSLATION_PREFIX + "enum." + configEntry.getType().getSimpleName() + "." + configEntry.get().toString()));
					}
					
					Button button = MakeBtn(
							widget.getValue().apply(configEntry.get()),
							optionFieldPosX, optionFieldPosZ,
							ConfigScreenConfigs.OPTION_FIELD_WIDTH, ConfigScreenConfigs.CATEGORY_BUTTON_HEIGHT,
							widget.getKey());
					
					// deactivate the button if the API is overriding it
					button.active = !configEntry.apiIsOverriding();
					
					
					this.configListWidget.addButton(this, configEntry,
							button,
							resetButton,
							null,
							textComponent);
					
					return true;
				}
				else if (((EntryInfo) configEntry.guiValue).widget != null)
				{
					// text box input
					
					EditBox widget = new EditBox(this.font,
							optionFieldPosX, optionFieldPosZ,
							ConfigScreenConfigs.OPTION_FIELD_WIDTH - 4, ConfigScreenConfigs.CATEGORY_BUTTON_HEIGHT,
							Translatable(""));
					widget.setMaxLength(ConfigScreenConfigs.OPTION_FIELD_WIDTH);
					widget.insertText(String.valueOf(configEntry.get()));
					
					Predicate<String> processor = ((BiFunction<EditBox, Button, Predicate<String>>) ((EntryInfo) configEntry.guiValue).widget).apply(widget, this.doneButton);
					widget.setFilter(processor);
					
					this.configListWidget.addButton(this, configEntry, widget, resetButton, null, textComponent);
					
					return true;
				}
			}
			
			return false;
		}
		private boolean tryCreateCategoryButton(AbstractConfigType configType)
		{
			if (configType instanceof ConfigCategory)
			{
				ConfigCategory configCategory = (ConfigCategory) configType;
				
				Component textComponent = this.GetTranslatableTextComponentForConfig(configCategory);
				
				int categoryPosX = this.width - ConfigScreenConfigs.CATEGORY_BUTTON_WIDTH - ConfigScreenConfigs.SPACE_FROM_RIGHT_SCREEN;
				int categoryPosZ = this.height - ConfigScreenConfigs.CATEGORY_BUTTON_HEIGHT; // Note: the posZ value here seems to be ignored
				
				Button widget = MakeBtn(textComponent,
						categoryPosX, categoryPosZ,
						ConfigScreenConfigs.CATEGORY_BUTTON_WIDTH, ConfigScreenConfigs.CATEGORY_BUTTON_HEIGHT,
						((button) ->
						{
							ConfigBase.INSTANCE.configFileHandler.saveToFile();
							Objects.requireNonNull(this.minecraft).setScreen(ClassicConfigGUI.getScreen(this.configBase, this, configCategory.getDestination()));
						}));
				this.configListWidget.addButton(this, configType, widget, null, null, null);
				
				return true;
			}
			
			return false;
		}
		private boolean tryCreateButton(AbstractConfigType configType)
		{
			if (configType instanceof ConfigUIButton)
			{
				ConfigUIButton configUiButton = (ConfigUIButton) configType;
				
				Component textComponent = this.GetTranslatableTextComponentForConfig(configUiButton);
				
				int buttonPosX = this.width - ConfigScreenConfigs.CATEGORY_BUTTON_WIDTH - ConfigScreenConfigs.SPACE_FROM_RIGHT_SCREEN;
				
				Button widget = MakeBtn(textComponent,
						buttonPosX, this.height - 28,
						ConfigScreenConfigs.CATEGORY_BUTTON_WIDTH, ConfigScreenConfigs.CATEGORY_BUTTON_HEIGHT,
						(button) -> ((ConfigUIButton) configType).runAction());
				this.configListWidget.addButton(this, configType, widget, null, null, null);
				
				return true;
			}
			
			return false;
		}
		private boolean tryCreateComment(AbstractConfigType configType)
		{
			if (configType instanceof ConfigUIComment)
			{
				ConfigUIComment configUiComment = (ConfigUIComment) configType;
			
				Component textComponent = this.GetTranslatableTextComponentForConfig(configUiComment);
				if (configUiComment.parentConfigPath != null)
				{
					textComponent = Translatable(TRANSLATION_PREFIX + configUiComment.parentConfigPath);
				}
				
				this.configListWidget.addButton(this, configType, null, null, null, textComponent);
				
				return true;
			}
			
			return false;
		}
		private boolean tryCreateSpacer(AbstractConfigType configType)
		{
			if (configType instanceof ConfigUISpacer)
			{
				Button spacerButton = MakeBtn(Translatable("distanthorizons.general.spacer"),
						10, 10, // having too small of a size causes division by 0 errors in older MC versions (IE 1.20.1)
						1, 1,
						(button) -> {});
				
				spacerButton.visible = false;
				this.configListWidget.addButton(this, configType, spacerButton, null, null, null);
				
				return true;
			}
			
			return false;
		}
		private boolean tryCreateLinkedEntry(AbstractConfigType configType)
		{
			if (configType instanceof ConfigUiLinkedEntry)
			{
				this.addMenuItem(((ConfigUiLinkedEntry) configType).get());
				
				return true;
			}
			
			return false;
		}
		
		private Component GetTranslatableTextComponentForConfig(AbstractConfigType configType)
		{ return Translatable(TRANSLATION_PREFIX + configType.getNameWCategory());}
		
		
		
		//===========//
		// rendering //
		//===========//
		
		@Override
        #if MC_VER < MC_1_20_1
		public void render(PoseStack matrices, int mouseX, int mouseY, float delta)
        #else
		public void render(GuiGraphics matrices, int mouseX, int mouseY, float delta)
		#endif
		{
			#if MC_VER < MC_1_20_2 // 1.20.2 now enables this by default in the `this.list.render` function
			this.renderBackground(matrices); // Renders background
			#else
			super.render(matrices, mouseX, mouseY, delta);
			#endif
			
			this.configListWidget.render(matrices, mouseX, mouseY, delta); // Render buttons
			
			
			// Render config title
			this.DhDrawCenteredString(matrices, this.font, this.title, 
					this.width / 2, 15, 
					#if MC_VER < MC_1_21_6 
					0xFFFFFF // RGB white
					#else 
					0xFFFFFFFF // ARGB white
					#endif);
			
			
			// render DH version
			this.DhDrawString(matrices, this.font, TextOrLiteral(ModInfo.VERSION), 2, this.height - 10, 
					#if MC_VER < MC_1_21_6
					0xAAAAAA // RGB white
					#else
					0xFFAAAAAA // ARGB white
					#endif);
			
			// If the update is pending, display this message to inform the user that it will apply when the game restarts
			if (SelfUpdater.deleteOldJarOnJvmShutdown)
			{
				this.DhDrawString(matrices, this.font, Translatable(ModInfo.ID + ".updater.waitingForClose"), 4, this.height - 42, 
						#if MC_VER < MC_1_21_6
						0xFFFFFF // RGB white
						#else
						0xFFFFFFFF // ARGB white
						#endif);
			}
			
			
			this.renderTooltip(matrices, mouseX, mouseY, delta);
			
			#if MC_VER < MC_1_20_2
			super.render(matrices, mouseX, mouseY, delta);
			#endif
		}
		
		#if MC_VER < MC_1_20_1
		private void renderTooltip(PoseStack matrices, int mouseX, int mouseY, float delta)
        #else
		private void renderTooltip(GuiGraphics matrices, int mouseX, int mouseY, float delta)
		#endif
		{
			AbstractWidget hoveredWidget = this.configListWidget.getHoveredButton(mouseX, mouseY);
			if (hoveredWidget == null)
			{
				return;
			}
			
			
			Component text = ButtonEntry.TEXT_BY_WIDGET.get(hoveredWidget);
			ButtonEntry button = ButtonEntry.BUTTON_BY_WIDGET.get(hoveredWidget);
			
			
			// A quick fix for tooltips on linked entries
			AbstractConfigType dhConfigType = ConfigUiLinkedEntry.class.isAssignableFrom(button.dhConfigType.getClass()) ?
					((ConfigUiLinkedEntry) button.dhConfigType).get() :
					button.dhConfigType;
			
			boolean apiOverrideActive = false;
			if (dhConfigType instanceof ConfigEntry)
			{
				apiOverrideActive = ((ConfigEntry)dhConfigType).apiIsOverriding();
			}
			
			Component name = Translatable(TRANSLATION_PREFIX + (dhConfigType.category.isEmpty() ? "" : dhConfigType.category + ".") + dhConfigType.getName());
			String key = TRANSLATION_PREFIX + (dhConfigType.category.isEmpty() ? "" : dhConfigType.category + ".") + dhConfigType.getName() + ".@tooltip";
			
			if (apiOverrideActive)
			{
				key = "distanthorizons.general.disabledByApi.@tooltip";
			}
			
			// display the validation error if present
			if (((EntryInfo) dhConfigType.guiValue).error != null)
			{ 
				this.DhRenderTooltip(matrices, this.font, ((EntryInfo) dhConfigType.guiValue).error.getValue(), mouseX, mouseY);
			}
			// display the tooltip if present
			else if (LANG_WRAPPER.langExists(key))
			{
				List<Component> list = new ArrayList<>();
				String lang = LANG_WRAPPER.getLang(key);
				for (String langLine : lang.split("\n"))
				{
					list.add(TextOrTranslatable(langLine));
				}
				
				this.DhRenderComponentTooltip(matrices, this.font, list, mouseX, mouseY);
			}
		}
		
		
		
		//==========//
		// shutdown //
		//==========//
		
		/** When you close it, it goes to the previous screen and saves */
		@Override
		public void onClose()
		{
			ConfigBase.INSTANCE.configFileHandler.saveToFile();
			Objects.requireNonNull(this.minecraft).setScreen(this.parent);
			
			CONFIG_CORE_INTERFACE.onScreenChangeListenerList.forEach((listener) -> listener.run());
		}
		
		
	}
	
	
	
	
	
	
	
	public static class ConfigListWidget extends ContainerObjectSelectionList<ButtonEntry>
	{
		Font textRenderer;
		
		public ConfigListWidget(Minecraft minecraftClient, int canvasWidth, int canvasHeight, int topMargin, int botMargin, int itemSpacing)
		{
			#if MC_VER < MC_1_20_4
			super(minecraftClient, canvasWidth, canvasHeight, topMargin, canvasHeight - botMargin, itemSpacing);
			#else
			super(minecraftClient, canvasWidth, canvasHeight - (topMargin + botMargin), topMargin, itemSpacing);
			#endif
			
			this.centerListVertically = false;
			this.textRenderer = minecraftClient.font;
		}
		
		public void addButton(ConfigScreen gui, AbstractConfigType dhConfigType, AbstractWidget button, AbstractWidget resetButton, AbstractWidget indexButton, Component text)
		{ this.addEntry(new ButtonEntry(gui, dhConfigType, button, text, resetButton, indexButton)); }
		
		@Override
		public int getRowWidth() { return 10_000; }
		
		public AbstractWidget getHoveredButton(double mouseX, double mouseY)
		{
			for (ButtonEntry buttonEntry : this.children())
			{
				AbstractWidget button = buttonEntry.button;
				if (button != null 
					&& button.visible)
				{
					#if MC_VER < MC_1_19_4
					double minX = button.x;
					double minY = button.y;
					#else
					double minX = button.getX();
					double minY = button.getY();
					#endif
					
					double maxX = minX + button.getWidth();
					double maxY = minY + button.getHeight();
					
					if (mouseX >= minX && mouseX < maxX
						&& mouseY >= minY && mouseY < maxY)
					{
						return button;
					}
				}
			}
			
			return null;
		}
		
	}
	
	
	public static class ButtonEntry extends ContainerObjectSelectionList.Entry<ButtonEntry>
	{
		private static final Font textRenderer = Minecraft.getInstance().font;
		
		private final AbstractWidget button;
		
		private final ConfigScreen gui;
		private final AbstractConfigType dhConfigType;
		
		private final AbstractWidget resetButton;
		private final AbstractWidget indexButton;
		private final Component text;
		private final List<AbstractWidget> children = new ArrayList<>();
		
		@NotNull
		private final EConfigCommentTextPosition textPosition;
		
		public static final Map<AbstractWidget, Component> TEXT_BY_WIDGET = new HashMap<>();
		// TODO we should just use a wrapper or something
		public static final Map<AbstractWidget, ButtonEntry> BUTTON_BY_WIDGET = new HashMap<>();
		
		
		
		public ButtonEntry(ConfigScreen gui, AbstractConfigType dhConfigType, 
				AbstractWidget button, Component text, AbstractWidget resetButton, AbstractWidget indexButton)
		{
			TEXT_BY_WIDGET.put(button, text);
			BUTTON_BY_WIDGET.put(button, this);
			
			this.gui = gui;
			this.dhConfigType = dhConfigType;
			
			this.button = button;
			this.resetButton = resetButton;
			this.text = text;
			this.indexButton = indexButton;
			
			if (button != null) { this.children.add(button); }
			if (resetButton != null) { this.children.add(resetButton); }
			if (indexButton != null) { this.children.add(indexButton); }
			
			
			EConfigCommentTextPosition textPosition = null;
			if (this.dhConfigType instanceof ConfigUIComment)
			{
				textPosition = ((ConfigUIComment)this.dhConfigType).textPosition;
			}
			
			if (textPosition == null)
			{
				if (this.button != null)
				{
					// if a button is present
					textPosition = EConfigCommentTextPosition.RIGHT_JUSTIFIED;
				}
				else
				{
					textPosition = EConfigCommentTextPosition.CENTERED_OVER_BUTTONS;
				}
			}
			this.textPosition = textPosition;
			
		}
		
		
		
		@Override
        #if MC_VER < MC_1_20_1
		public void render(PoseStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta)
        #elif MC_VER < MC_1_21_9
		public void render(GuiGraphics matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta)
		#else
		public void renderContent(GuiGraphics matrices, int mouseX, int mouseY, boolean hovered, float tickDelta)
		#endif
		{
			try
			{
				#if MC_VER < MC_1_21_9
				#else
				int y = this.getY(); // TODO why is the Y value being set during render?
				#endif
				
				if (this.button != null)
				{
					SetY(this.button, y);
					this.button.render(matrices, mouseX, mouseY, tickDelta);
				}
				
				if (this.resetButton != null)
				{
					SetY(this.resetButton, y);
					this.resetButton.render(matrices, mouseX, mouseY, tickDelta);
				}
				
				if (this.indexButton != null)
				{
					SetY(this.indexButton, y);
					this.indexButton.render(matrices, mouseX, mouseY, tickDelta);
				}
				
				if (this.text != null)
				{
					int translatedLength = textRenderer.width(this.text);
					
					int textXPos;
					if (this.textPosition == EConfigCommentTextPosition.RIGHT_JUSTIFIED)
					{
						// text right justified aligned against the buttons
						textXPos = this.gui.width
								- translatedLength
								- ConfigScreenConfigs.SPACE_BETWEEN_TEXT_AND_OPTION_FIELD
								- ConfigScreenConfigs.SPACE_FROM_RIGHT_SCREEN
								- ConfigScreenConfigs.OPTION_FIELD_WIDTH
								- ConfigScreenConfigs.BUTTON_WIDTH_SPACING
								- ConfigScreenConfigs.RESET_BUTTON_WIDTH;
					}
					else if (this.textPosition == EConfigCommentTextPosition.CENTERED_OVER_BUTTONS)
					{
						// have button centered relative to a category button
						textXPos = this.gui.width
								- (translatedLength / 2)
								- (ConfigScreenConfigs.CATEGORY_BUTTON_WIDTH / 2)
								- ConfigScreenConfigs.SPACE_FROM_RIGHT_SCREEN;
					}
					else if (this.textPosition == EConfigCommentTextPosition.CENTER_OF_SCREEN)
					{
						// have button centered in the screen
						textXPos = (this.gui.width / 2)
								- (translatedLength / 2);
					}
					else
					{
						throw new UnsupportedOperationException("No text position render defined for [" + this.textPosition + "]");
					}
				
				
                #if MC_VER < MC_1_20_1
				GuiComponent.drawString(matrices, textRenderer, 
					this.text, 
					textXPos, y + 5, 
					0xFFFFFF);
				#elif MC_VER < MC_1_21_6
					matrices.drawString(textRenderer,
							this.text,
							textXPos, y + 5,
							0xFFFFFF);
				#else
				matrices.drawString(textRenderer, 
						this.text,
						textXPos, y + 5, 
						0xFFFFFFFF);
				#endif
				}
			}
			catch (Exception e)
			{
				// should prevent crashing the game if there's an issue
				SPAM_LOGGER.error("Unexpected gui rendering issue: ["+e.getMessage()+"]", e);
			}
		}
		
		@Override
		public @NotNull List<? extends GuiEventListener> children()
		{ return this.children; }
		
		#if MC_VER >= MC_1_17_1
		@Override
		public @NotNull List<? extends NarratableEntry> narratables()
		{ return this.children; }
		#endif
		
		
		
	}
	
	
	
	
	
	//================//
	// event handling //
	//================//
	
	private static class ConfigCoreInterface implements IConfigGui
	{
		/**
		 * in the future it would be good to pass in the current page and other variables, 
		 * but for now just knowing when the page is closed is good enough 
		 */
		public final ArrayList<Runnable> onScreenChangeListenerList = new ArrayList<>();
		
		
		
		@Override
		public void addOnScreenChangeListener(Runnable newListener) { this.onScreenChangeListenerList.add(newListener); }
		@Override
		public void removeOnScreenChangeListener(Runnable oldListener) { this.onScreenChangeListenerList.remove(oldListener); }
		
	}
	
}
