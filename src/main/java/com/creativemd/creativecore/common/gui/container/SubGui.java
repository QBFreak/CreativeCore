package com.creativemd.creativecore.common.gui.container;

import com.creativemd.creativecore.common.event.CreativeCoreEventBus;
import com.creativemd.creativecore.common.gui.GuiRenderHelper;
import com.creativemd.creativecore.common.gui.client.style.Style;
import com.creativemd.creativecore.common.gui.event.ControlEvent;
import com.creativemd.creativecore.common.gui.mc.ContainerSub;
import com.creativemd.creativecore.common.gui.mc.GuiContainerSub;
import com.creativemd.creativecore.common.gui.premade.SubContainerEmpty;
import com.creativemd.creativecore.common.gui.premade.SubGuiDialog;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.packet.gui.GuiLayerPacket;
import com.creativemd.creativecore.common.packet.gui.GuiUpdatePacket;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public abstract class SubGui extends GuiParent {
	
	public SubContainer container;
	
	public GuiContainerSub gui;
	
	private CreativeCoreEventBus eventBus;
	
	public SubGui() {
		this(176, 166);
	}
	
	public SubGui(int width, int height) {
		this("gui", width, height);
	}
	
	public SubGui(String name, int width, int height) {
		super(name, 0, 0, width, height);
		eventBus = new CreativeCoreEventBus(true);
		eventBus.RegisterEventListener(this);
	}
	
	// ================Layers================
	
	public boolean isTopLayer() {
		return gui.isTopLayer(this);
	}
	
	public int getLayerID() {
		return gui.getLayers().indexOf(this);
	}
	
	public void openNonSyncedLayer(SubGui gui) {
		gui.container = new SubContainerEmpty(getPlayer());
		gui.container.container = (ContainerSub) this.gui.inventorySlots;
		gui.gui = this.gui;
		gui.onOpened();
		this.gui.addLayer(gui);
		
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setBoolean("newNonSyncedLayer", true);
		sendPacketToServer(nbt);
	}
	
	public void openNewLayer(NBTTagCompound nbt) {
		openNewLayer(nbt, false);
	}
	
	public void openNewLayer(NBTTagCompound nbt, boolean isPacket) {
		gui.addLayer(createLayer(mc.world, mc.player, nbt));
		if (!isPacket)
			PacketHandler.sendPacketToServer(new GuiLayerPacket(nbt, getLayerID(), false));
	}
	
	public void closeLayer(NBTTagCompound nbt) {
		closeLayer(nbt, false);
	}
	
	public void closeLayer(NBTTagCompound nbt, boolean isPacket) {
		onClosed();
		if (!isPacket)
			PacketHandler.sendPacketToServer(new GuiLayerPacket(nbt, getLayerID(), true));
		gui.removeLayer(this);
		if (gui.hasTopLayer())
			gui.getTopLayer().onLayerClosed(this, nbt);
	}
	
	public void onLayerClosed(SubGui gui, NBTTagCompound nbt) {
		if (nbt.getBoolean("dialog")) {
			String[] buttons = new String[nbt.getInteger("count")];
			for (int i = 0; i < buttons.length; i++) {
				buttons[i] = nbt.getString("b" + i);
			}
			onDialogClosed(nbt.getString("text"), buttons, nbt.getString("clicked"));
		}
	}
	
	public void closeGui() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setBoolean("exit", true);
		closeLayer(nbt);
		if (gui.getLayers().size() == 0)
			mc.player.closeScreen();
	}
	
	public SubGui createLayer(World world, EntityPlayer player, NBTTagCompound nbt) {
		SubGui layer = createLayerFromPacket(world, player, nbt);
		layer.container = container.createLayerFromPacket(world, player, nbt);
		layer.gui = gui;
		layer.onOpened();
		return layer;
	}
	
	public SubGui createLayerFromPacket(World world, EntityPlayer player, NBTTagCompound nbt) {
		if (nbt.getBoolean("dialog")) {
			String[] buttons = new String[nbt.getInteger("count")];
			for (int i = 0; i < buttons.length; i++) {
				buttons[i] = nbt.getString("b" + i);
			}
			return new SubGuiDialog(nbt.getString("text").split("\n"), buttons);
		}
		return null;
	}
	
	// ================DIALOGS================
	
	public void openYesNoDialog(String text) {
		openButtonDialogDialog(text, "Yes", "No");
	}
	
	public void openButtonDialogDialog(String text, String... buttons) {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setBoolean("dialog", true);
		nbt.setString("text", text);
		nbt.setInteger("count", buttons.length);
		for (int i = 0; i < buttons.length; i++) {
			nbt.setString("b" + i, buttons[i]);
		}
		openNewLayer(nbt);
	}
	
	public void openSaveDialog(String text) {
		openButtonDialogDialog(text, "Yes", "No", "Cancel");
	}
	
	public void onDialogClosed(String text, String[] buttons, String clicked) {
		
	}
	
	// ================Interaction================
	
	@Override
	public boolean isInteractable() {
		return super.isInteractable() && isTopLayer();
	}
	
	public boolean closeGuiUsingEscape() {
		return true;
	}
	
	@Override
	public boolean onKeyPressed(char character, int key) {
		if (key == 1) {
			if (closeGuiUsingEscape())
				closeGui();
			return true;
		}
		if (super.onKeyPressed(character, key))
			return true;
		if (key == this.mc.gameSettings.keyBindInventory.getKeyCode()) {
			closeGui();
			return true;
		}
		return false;
	}
	
	// ================Positioning================
	
	@Override
	public int getPixelOffsetX() {
		return gui.getGuiLeft() + getContentOffset();
	}
	
	@Override
	public int getPixelOffsetY() {
		return gui.getGuiTop() + getContentOffset();
	}
	
	// ================NETWORK================
	
	/* public void readFromOpeningNBT(NBTTagCompound nbt){} */
	
	public void receiveContainerPacket(NBTTagCompound nbt) {
	}
	
	public void sendPacketToServer(NBTTagCompound nbt) {
		PacketHandler.sendPacketToServer(new GuiUpdatePacket(nbt, false, getLayerID()));
	}
	
	// ================Helper================
	
	@Override
	public EntityPlayer getPlayer() {
		return container.player;
	}
	
	// ================Controls================
	
	public abstract void createControls();
	
	// ================Rendering================
	
	@Override
	protected void renderBackground(GuiRenderHelper helper, Style style) {
		style.getBorder(this).renderStyle(helper, width, height);
		GlStateManager.translate(borderWidth, borderWidth, 0);
		style.getFace(this).renderStyle(helper, width - borderWidth * 2, height - borderWidth * 2);
	}
	
	@Override
	public boolean isMouseOver() {
		if (parent != null)
			return super.isMouseOver();
		return isTopLayer();
	}
	
	public boolean hasGrayBackground() {
		return true;
	}
	
	// ================CUSTOM EVENTS================
	
	public void onTick() {
	}
	
	@Override
	public void onClosed() {
		super.onClosed();
		eventBus.removeAllEventListeners();
	}
	
	public void addContainerControls() {
		for (int i = 0; i < container.controls.size(); i++) {
			container.controls.get(i).onOpened();
			controls.add(container.controls.get(i).getGuiControl());
		}
	}
	
	@Override
	public void onOpened() {
		createControls();
		for (int i = 0; i < controls.size(); i++) {
			controls.get(i).parent = this;
			controls.get(i).onOpened();
		}
		
		if (container != null) {
			addContainerControls();
		}
		refreshControls();
	}
	
	// ================Internal Events================
	
	public boolean raiseEvent(ControlEvent event) {
		return !eventBus.raiseEvent(event);
	}
	
	public void addListener(Object listener) {
		eventBus.RegisterEventListener(listener);
	}
	
	public void removeListener(Object listener) {
		eventBus.removeEventListener(listener);
	}
	
}
