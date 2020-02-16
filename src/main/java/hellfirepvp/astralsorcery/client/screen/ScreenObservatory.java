/*******************************************************************************
 * HellFirePvP / Astral Sorcery 2020
 *
 * All rights reserved.
 * The source code is available on github: https://github.com/HellFirePvP/AstralSorcery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.astralsorcery.client.screen;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GlStateManager;
import hellfirepvp.astralsorcery.client.ClientScheduler;
import hellfirepvp.astralsorcery.client.lib.TexturesAS;
import hellfirepvp.astralsorcery.client.screen.base.ConstellationDiscoveryScreen;
import hellfirepvp.astralsorcery.client.screen.base.SkyScreen;
import hellfirepvp.astralsorcery.client.screen.base.TileConstellationDiscoveryScreen;
import hellfirepvp.astralsorcery.client.screen.telescope.FullScreenDrawArea;
import hellfirepvp.astralsorcery.client.screen.telescope.PlayerAngledConstellationInformation;
import hellfirepvp.astralsorcery.client.util.Blending;
import hellfirepvp.astralsorcery.client.util.RenderingConstellationUtils;
import hellfirepvp.astralsorcery.client.util.RenderingDrawUtils;
import hellfirepvp.astralsorcery.common.constellation.IConstellation;
import hellfirepvp.astralsorcery.common.constellation.SkyHandler;
import hellfirepvp.astralsorcery.common.constellation.star.StarLocation;
import hellfirepvp.astralsorcery.common.constellation.world.DayTimeHelper;
import hellfirepvp.astralsorcery.common.constellation.world.WorldContext;
import hellfirepvp.astralsorcery.common.container.ContainerObservatory;
import hellfirepvp.astralsorcery.common.data.research.ResearchHelper;
import hellfirepvp.astralsorcery.common.event.EventFlags;
import hellfirepvp.astralsorcery.common.tile.TileObservatory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IHasContainer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.LogicalSide;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: ScreenObservatory
 * Created by HellFirePvP
 * Date: 15.02.2020 / 18:27
 */
public class ScreenObservatory extends TileConstellationDiscoveryScreen<TileObservatory, ConstellationDiscoveryScreen.DrawArea> implements IHasContainer<ContainerObservatory> {

    private static final Random RAND = new Random();
    private static final int FRAME_TEXTURE_SIZE = 16;

    private static final int randomStars = 220;
    private List<Point.Float> usedStars = new ArrayList<>(randomStars);
    private final ContainerObservatory container;

    public ScreenObservatory(ContainerObservatory container) {
        super(container.getTileEntity(),
                Minecraft.getInstance().mainWindow.getScaledHeight() - FRAME_TEXTURE_SIZE * 2,
                Minecraft.getInstance().mainWindow.getScaledWidth() - FRAME_TEXTURE_SIZE * 2);
        this.container = container;

        PlayerEntity player = Minecraft.getInstance().player;
        if (player != null) {
            TileObservatory observatory = this.getTile();
            player.rotationPitch     = observatory.observatoryPitch;
            player.prevRotationPitch = observatory.prevObservatoryPitch;
            player.rotationYaw       = observatory.observatoryYaw;
            player.prevRotationYaw   = observatory.prevObservatoryYaw;
        }
    }

    @Override
    public ContainerObservatory getContainer() {
        return container;
    }

    @Nonnull
    @Override
    protected List<DrawArea> createDrawAreas() {
        return Lists.newArrayList(FullScreenDrawArea.INSTANCE);
    }

    @Override
    protected void fillConstellations(WorldContext ctx, List<DrawArea> drawAreas) {
        DrawArea area = drawAreas.get(0);
        Random gen = ctx.getDayRandom();

        Map<IConstellation, Point.Float> placed = new HashMap<>();
        for (IConstellation cst : ctx.getActiveCelestialsHandler().getActiveConstellations()) {
            Point.Float foundPoint;
            do {
                foundPoint = tryEmptyPlace(placed.values(), gen);
            } while (foundPoint == null);
            area.addConstellationToArea(cst, new PlayerAngledConstellationInformation(DEFAULT_CONSTELLATION_SIZE, foundPoint.y, foundPoint.x));
        }

        for (int i = 0; i < randomStars; i++) {
            usedStars.add(new Point.Float(FRAME_TEXTURE_SIZE + gen.nextFloat() * this.getGuiWidth(), FRAME_TEXTURE_SIZE + gen.nextFloat() * this.getGuiHeight()));
        }
    }

    private Point.Float tryEmptyPlace(Collection<Point.Float> placed, Random gen) {
        double constellationGap = 12.0;
        constellationGap = Math.sqrt(constellationGap * constellationGap * 2);

        float rPitch = -6.5F + gen.nextFloat() * - 80F;
        float rYaw = gen.nextFloat() * 360F;
        for (Point.Float point : placed) {
            if (point.distance(rPitch, rYaw) <= constellationGap ||
                    point.distance(rPitch, rYaw - 360F) <= constellationGap) {
                return null;
            }
        }
        return new Point.Float(rPitch, rYaw);
    }

    @Override
    public void removed() {
        super.removed();
        EventFlags.GUI_CLOSING.executeWithFlag(() -> Minecraft.getInstance().player.closeScreen());
    }

    @Override
    public void render(int mouseX, int mouseY, float pTicks) {
        super.render(mouseX, mouseY, pTicks);

        Minecraft.getInstance().gameSettings.thirdPersonView = 0;

        double guiFactor = Minecraft.getInstance().mainWindow.getGuiScaleFactor();
        this.blitOffset -= 10;
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(MathHelper.floor((FRAME_TEXTURE_SIZE - 2) * guiFactor),
                MathHelper.floor((FRAME_TEXTURE_SIZE - 2) * guiFactor),
                MathHelper.floor((this.getGuiWidth() + 2) * guiFactor),
                MathHelper.floor((this.getGuiHeight() + 2) * guiFactor));
        this.drawObservatoryScreen(pTicks);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        this.blitOffset += 10;

        this.blitOffset += 10;
        drawFrame();
        this.blitOffset -= 10;
    }

    private void drawObservatoryScreen(float pTicks) {
        boolean canSeeSky = this.canObserverSeeSky(this.getTile().getPos(), 2);
        float pitch = Minecraft.getInstance().player.getPitch(pTicks);
        float angleOpacity = 0F;
        if (pitch < -60F) {
            angleOpacity = 1F;
        } else if (pitch < -10F) {
            angleOpacity = (Math.abs(pitch) - 10F) / 50F;
            if (DayTimeHelper.isNight(Minecraft.getInstance().world)) {
                angleOpacity *= angleOpacity;
            }
        }
        float brMultiplier = angleOpacity;

        GlStateManager.enableBlend();
        Blending.DEFAULT.applyStateManager();
        GlStateManager.disableAlphaTest();

        this.drawSkyBackground(pTicks, canSeeSky, angleOpacity);

        if (!this.isInitialized()) {
            GlStateManager.enableAlphaTest();
            GlStateManager.disableBlend();
            return;
        }

        float playerYaw = Minecraft.getInstance().player.rotationYaw % 360F;
        if (playerYaw < 0) {
            playerYaw += 360F;
        }
        if (playerYaw >= 180F) {
            playerYaw -= 360F;
        }
        float playerPitch = Minecraft.getInstance().player.rotationPitch;
        float rainBr = 1F - Minecraft.getInstance().world.getRainStrength(pTicks);
        float cstSizeX = 55F;
        float cstSizeY = 35F;

        this.blitOffset += 1;
        WorldContext ctx = SkyHandler.getContext(Minecraft.getInstance().world, LogicalSide.CLIENT);
        if (ctx != null && canSeeSky) {
            Random gen = ctx.getDayRandom();
            Tessellator tes = Tessellator.getInstance();
            BufferBuilder buf = tes.getBuffer();

            buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            TexturesAS.TEX_STAR_1.bindTexture();
            for (Point.Float star : usedStars) {
                float size = 3 + gen.nextFloat() * 3F;
                float brightness = 0.4F + (RenderingConstellationUtils.stdFlicker(ClientScheduler.getClientTick(), pTicks, 10 + gen.nextInt(20))) * 0.5F;
                brightness = this.multiplyStarBrightness(pTicks, brightness);
                brightness *= brMultiplier;
                this.drawRect(buf).at(star.x, star.y).dim(size, size).color(brightness, brightness, brightness, brightness).draw();
            }
            tes.draw();
            this.blitOffset -= 1;

            this.blitOffset += 3;
            for (DrawArea area : this.getVisibleDrawAreas()) {
                for (IConstellation cst : area.getDisplayMap().keySet()) {
                    ConstellationDisplayInformation info = area.getDisplayMap().get(cst);
                    info.getFrameDrawInformation().clear();
                    if (!(info instanceof PlayerAngledConstellationInformation)) {
                        continue;
                    }
                    PlayerAngledConstellationInformation cstInfo = (PlayerAngledConstellationInformation) info;
                    float size = cstInfo.getRenderSize();
                    float diffYaw = playerYaw - cstInfo.getYaw();
                    float diffPitch = playerPitch - cstInfo.getPitch();

                    if ((Math.abs(diffYaw) <= size || Math.abs(diffYaw += 360F) <= size) &&
                            Math.abs(diffPitch) <= size) {
                        int wPart = MathHelper.floor(this.getGuiWidth() * 0.1F);
                        int hPart = MathHelper.floor(this.getGuiHeight() * 0.1F);

                        Map<StarLocation, Rectangle> cstRenderInfo = RenderingConstellationUtils.renderConstellationIntoGUI(
                                cst,
                                this.getGuiLeft() + wPart + MathHelper.floor((diffYaw / cstSizeX) * this.getGuiWidth()),
                                this.getGuiTop() + hPart + MathHelper.floor((diffPitch / cstSizeY) * this.getGuiHeight()),
                                this.blitOffset,
                                MathHelper.floor(this.getGuiHeight() * 0.6F),
                                 MathHelper.floor(this.getGuiHeight() * 0.6F),
                                2F,
                                () -> (0.4F + 0.6F * RenderingConstellationUtils.conCFlicker(ClientScheduler.getClientTick(), pTicks, 5 + gen.nextInt(15))) * rainBr * brMultiplier,
                                ResearchHelper.getClientProgress().hasConstellationDiscovered(cst),
                                true
                        );

                        cstInfo.getFrameDrawInformation().putAll(cstRenderInfo);
                    }
                }
            }
            this.blitOffset -= 3;

            this.blitOffset += 5;
            this.renderDrawnLines(gen, pTicks);
            this.blitOffset -= 5;
        }

        GlStateManager.enableAlphaTest();
        GlStateManager.disableBlend();
    }

    private void drawFrame() {
        TexturesAS.TEX_GUI_OBSERVATORY.bindTexture();

        Tessellator tes = Tessellator.getInstance();
        BufferBuilder buf = tes.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

        this.drawRect(buf).at(0, 0).dim(FRAME_TEXTURE_SIZE, FRAME_TEXTURE_SIZE)
                .tex(0, 0, 8F / 20F, 8F / 20F).draw();
        this.drawRect(buf).at(this.getGuiWidth() + FRAME_TEXTURE_SIZE, 0).dim(FRAME_TEXTURE_SIZE, FRAME_TEXTURE_SIZE)
                .tex(8F / 20F, 0, 8F / 20F, 8F / 20F).draw();
        this.drawRect(buf).at(this.getGuiWidth() + FRAME_TEXTURE_SIZE, this.getGuiHeight() + FRAME_TEXTURE_SIZE).dim(FRAME_TEXTURE_SIZE, FRAME_TEXTURE_SIZE)
                .tex(8F / 20F, 8F / 20F, 8F / 20F, 8F / 20F).draw();
        this.drawRect(buf).at(0, this.getGuiHeight() + FRAME_TEXTURE_SIZE).dim(FRAME_TEXTURE_SIZE, FRAME_TEXTURE_SIZE)
                .tex(0, 8F / 20F, 8F / 20F, 8F / 20F).draw();

        this.drawRect(buf).at(FRAME_TEXTURE_SIZE, 0).dim(this.getGuiWidth(), FRAME_TEXTURE_SIZE)
                .tex(16F / 20F, 0, 1F / 20F, 8F / 20F).draw();
        this.drawRect(buf).at(this.getGuiWidth() + FRAME_TEXTURE_SIZE, FRAME_TEXTURE_SIZE).dim(FRAME_TEXTURE_SIZE, this.getGuiHeight())
                .tex(0, 17F / 20F, 8F / 20F, 1F / 20F).draw();
        this.drawRect(buf).at(FRAME_TEXTURE_SIZE, this.getGuiHeight() + FRAME_TEXTURE_SIZE).dim(this.getGuiWidth(), FRAME_TEXTURE_SIZE)
                .tex(17F / 20F, 0, 1F / 20F, 8F / 20F).draw();
        this.drawRect(buf).at(0, FRAME_TEXTURE_SIZE).dim(FRAME_TEXTURE_SIZE, this.getGuiHeight())
                .tex(0, 16F / 20F, 8F / 20F, 1F / 20F).draw();

        tes.draw();
    }

    private void drawSkyBackground(float pTicks, boolean canSeeSky, float angleOpacity) {
        Tuple<Color, Color> rgbFromTo = SkyScreen.getSkyGradient(canSeeSky, angleOpacity, pTicks);
        RenderingDrawUtils.drawGradientRect(this.blitOffset,
                this.guiLeft, this.guiTop,
                this.guiLeft + this.guiWidth, this.guiTop + this.guiHeight,
                rgbFromTo.getA().getRGB(), rgbFromTo.getB().getRGB());
    }

    @Override
    public void mouseMoved(double xPos, double yPos) {
        if (!Minecraft.getInstance().mouseHelper.isMouseGrabbed()) {
            return;
        }

        int offsetX = 6, offsetY = 6;
        int width = guiWidth - 12, height = guiHeight - 12;

        Minecraft mc = Minecraft.getInstance();
        double xDiff = mc.mouseHelper.getMouseX() - (xPos / ((double) mc.mainWindow.getScaledWidth()  / mc.mainWindow.getWidth()));
        double yDiff = mc.mouseHelper.getMouseY() - (yPos / ((double) mc.mainWindow.getScaledHeight() / mc.mainWindow.getHeight()));

        float pitch = Minecraft.getInstance().player.rotationPitch;
        if (pitch <= -89.99F && yDiff > 0) {
            yDiff = 0;
        }
        if (pitch >= -10F) {
            Minecraft.getInstance().player.rotationPitch = -10F;
            yDiff = 0;
        }
        if (pitch <= -75F) {
            Minecraft.getInstance().player.rotationPitch = -75F;
            yDiff = 0;
        }

        for (Point.Float sl : usedStars) {
            sl.x -= xDiff;
            sl.y += yDiff;

            if (sl.x < offsetX) {
                sl.x += width;
            } else if (sl.x > (offsetX + width)) {
                sl.x -= width;
            }
            if (sl.y < offsetY) {
                sl.y += height;
            } else if (sl.y > (offsetY + height)) {
                sl.y -= height;
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected boolean shouldRightClickCloseScreen(double mouseX, double mouseY) {
        return true;
    }
}