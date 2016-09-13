package hellfirepvp.astralsorcery.client.gui.journal;

import hellfirepvp.astralsorcery.client.gui.GuiJournalProgression;
import hellfirepvp.astralsorcery.client.util.AssetLibrary;
import hellfirepvp.astralsorcery.client.util.AssetLoader;
import hellfirepvp.astralsorcery.client.util.BindableResource;
import hellfirepvp.astralsorcery.client.util.ClientJournalMapping;
import hellfirepvp.astralsorcery.common.data.research.PlayerProgress;
import hellfirepvp.astralsorcery.common.data.research.ResearchManager;
import hellfirepvp.astralsorcery.common.data.research.ResearchProgression;
import hellfirepvp.astralsorcery.common.util.data.Vector3;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: GuiProgressionRenderer
 * Created by HellFirePvP
 * Date: 12.08.2016 / 17:31
 */
public class GuiProgressionRenderer {

    private static final BindableResource textureResBack = AssetLibrary.loadTexture(AssetLoader.TextureLocation.GUI, "guiJResBG");
    private static final BindableResource textureResOVL = AssetLibrary.loadTexture(AssetLoader.TextureLocation.GUI, "guiJResOverlay");
    //private static final BindableResource black = AssetLibrary.loadTexture(AssetLoader.TextureLocation.MISC, "black");

    private GalaxySizeHandler sizeHandler;
    private GuiJournalProgression parentGui;

    //Rendering relevant properties. Those coords represent _unscaled_ exact positions.
    public GuiRenderBoundingBox realRenderBox;
    private int realCoordLowerX, realCoordLowerY;
    private int realRenderWidth, realRenderHeight;

    private ScalingPoint mousePointScaled;
    private ScalingPoint previousMousePointScaled;

    private ResearchProgression focusedCluster = null;
    private GuiProgressionClusterRenderer clusterRenderer = null;

    private boolean hasPrevOffset = false;
    private Map<Rectangle, ResearchProgression> clusterRectMap = new HashMap<>();

    public GuiProgressionRenderer(GuiJournalProgression gui, int guiHeight, int guiWidth) {
        this.parentGui = gui;
        this.sizeHandler = new GalaxySizeHandler(guiHeight, guiWidth);
        this.mousePointScaled = ScalingPoint.createPoint(
                this.sizeHandler.clampX(this.sizeHandler.getMidX()),
                this.sizeHandler.clampY(this.sizeHandler.getMidY()),
                this.sizeHandler.getScalingFactor(),
                false);
    }

    public void refreshSize() {
        this.sizeHandler.updateSize();
    }

    public void setBox(int left, int top, int right, int bottom) {
        this.realRenderBox = new GuiRenderBoundingBox(left, top, right, bottom);
        this.realRenderWidth = (int) this.realRenderBox.getWidth();
        this.realRenderHeight = (int) this.realRenderBox.getHeight();
    }

    public void moveMouse(double changedX, double changedY) {
        if(sizeHandler.getScalingFactor() >= 6.1D && clusterRenderer != null) {
            clusterRenderer.moveMouse(changedX, changedY);
        } else {
            if (hasPrevOffset) {
                mousePointScaled.updateScaledPos(
                        sizeHandler.clampX(previousMousePointScaled.getScaledPosX() + changedX),
                        sizeHandler.clampY(previousMousePointScaled.getScaledPosY() + changedY),
                        sizeHandler.getScalingFactor());
            } else {
                mousePointScaled.updateScaledPos(
                        sizeHandler.clampX(changedX),
                        sizeHandler.clampY(changedY),
                        sizeHandler.getScalingFactor());
            }
        }
    }

    public void applyMovedMouseOffset() {
        if(sizeHandler.getScalingFactor() >= 6.1D && clusterRenderer != null) {
            clusterRenderer.applyMovedMouseOffset();
        } else {
            this.previousMousePointScaled = ScalingPoint.createPoint(
                    mousePointScaled.getScaledPosX(),
                    mousePointScaled.getScaledPosY(),
                    sizeHandler.getScalingFactor(),
                    true);
            this.hasPrevOffset = true;
        }
    }

    public void updateOffset(int guiLeft, int guiTop) {
        this.realCoordLowerX = guiLeft;
        this.realCoordLowerY = guiTop;
    }

    public void centerMouse() {
        moveMouse(sizeHandler.getMidX(), sizeHandler.getMidY());
    }

    public void updateMouseState() {
        moveMouse(0, 0);
    }

    public void unfocus() {
        focusedCluster = null;
    }

    public void focus(@Nonnull ResearchProgression researchCluster) {
        this.focusedCluster = researchCluster;
        this.clusterRenderer = new GuiProgressionClusterRenderer(researchCluster, realRenderHeight, realRenderWidth, realCoordLowerX, realCoordLowerY);
    }

    //Nothing to actually click here, we redirect if we can.
    public void propagateClick(Point p) {
        if(clusterRenderer != null && sizeHandler.getScalingFactor() > 6) {
            clusterRenderer.propagateClick(p);
        }
    }

    public void drawMouseHighlight(float zLevel, Point mousePoint) {
        if(clusterRenderer != null && sizeHandler.getScalingFactor() > 6) {
            clusterRenderer.drawMouseHighlight(zLevel, mousePoint);
        }
    }

    public void resetZoom() {
        sizeHandler.resetZoom();
        rescale(sizeHandler.getScalingFactor());
    }

    public void handleZoomOut() {
        this.sizeHandler.handleZoomOut();
        rescale(sizeHandler.getScalingFactor());

        if(this.sizeHandler.getScalingFactor() <= 4.0) {
            unfocus();
        } else if(this.sizeHandler.getScalingFactor() >= 6.0 && this.clusterRenderer != null) {
            clusterRenderer.handleZoomOut();
        }
    }

    /**
     * Thresholds for zooming in:
     * 1.0 - 4.0 don't care.
     * 4.0 - 6.0 has to have focus + centering to center of cluster
     * 6.0 - 10.0 transition (6.0 - 8.0) + cluster rendering + handling (cursor movement)
     */
    public void handleZoomIn() {
        double scale = sizeHandler.getScalingFactor();
        //double nextScale = Math.min(10.0D, scale + 0.2D);
        if(scale >= 4.0D) {
            if(focusedCluster == null) {
                ResearchProgression prog = tryFocusCluster();
                if(prog != null) {
                    focus(prog);
                }
            }
            if(focusedCluster == null) {
                return;
            }
            if (scale < 6.1D) { //Floating point shenanigans
                double vDiv = (2D - (scale - 4.0D)) * 10D;
                Rectangle2D rect = calcBoundingRectangle(focusedCluster);
                Vector3 center = new Vector3(rect.getCenterX(), rect.getCenterY(), 0);
                Vector3 mousePos = new Vector3(mousePointScaled.getScaledPosX(), mousePointScaled.getScaledPosY(), 0);
                Vector3 dir = center.subtract(mousePos);
                if(vDiv > 0.05) {
                    dir.divide(vDiv);
                }
                if(!hasPrevOffset) {
                    mousePointScaled.updateScaledPos(
                            sizeHandler.clampX(mousePos.getX() + dir.getX()),
                            sizeHandler.clampY(mousePos.getY() + dir.getY()),
                            sizeHandler.getScalingFactor());
                } else {
                    previousMousePointScaled.updateScaledPos(
                            sizeHandler.clampX(mousePos.getX() + dir.getX()),
                            sizeHandler.clampY(mousePos.getY() + dir.getY()),
                            sizeHandler.getScalingFactor());
                }

                updateMouseState();
            } else if(clusterRenderer != null) {
                clusterRenderer.handleZoomIn();
            }
        }
        this.sizeHandler.handleZoomIn();
        rescale(sizeHandler.getScalingFactor());
    }

    private void rescale(double newScale) {
        this.mousePointScaled.rescale(newScale);
        if(this.previousMousePointScaled != null) {
            this.previousMousePointScaled.rescale(newScale);
        }
        updateMouseState();
    }

    public void drawProgressionPart(float zLevel) {
        drawBaseBackground(zLevel);

        drawClusters(zLevel);

        double scaleX = this.mousePointScaled.getPosX();
        double scaleY = this.mousePointScaled.getPosY();

        if(sizeHandler.getScalingFactor() >= 6.1D && focusedCluster != null && clusterRenderer != null) {
            ClientJournalMapping.JournalCluster cluster = ClientJournalMapping.getClusterMapping(focusedCluster);
            drawClusterBackground(cluster.clusterBackgroundTexture, zLevel);

            clusterRenderer.drawClusterScreen(zLevel);
            scaleX = clusterRenderer.getScaleMouseX();
            scaleY = clusterRenderer.getScaleMouseY();
        }

        drawBlendedStarfieldLayers(scaleX, scaleY, zLevel);
    }

    @Nullable
    private ResearchProgression tryFocusCluster() {
        Point mousePoint = parentGui.getCurrentMousePoint();

        for (Rectangle r : this.clusterRectMap.keySet()) {
            if(r.contains(mousePoint)) {
                return this.clusterRectMap.get(r);
            }
        }
        return null;
    }

    private Rectangle2D calcBoundingRectangle(ResearchProgression progression) {
        ClientJournalMapping.JournalCluster cluster = ClientJournalMapping.getClusterMapping(progression);
        double lX = sizeHandler.evRelativePosX(cluster.leftMost);
        double rX = sizeHandler.evRelativePosX(cluster.rightMost);
        double lY = sizeHandler.evRelativePosY(cluster.upperMost);
        double rY = sizeHandler.evRelativePosY(cluster.lowerMost);
        return new Rectangle2D.Double(lX, lY, rX - lX, rY - lY);
    }

    private void drawClusters(float zLevel) {
        clusterRectMap.clear();
        if(sizeHandler.getScalingFactor() >= 8.01) return;

        PlayerProgress thisProgress = ResearchManager.clientProgress;
        for (ResearchProgression progress : thisProgress.getResearchProgression()) {
            ClientJournalMapping.JournalCluster cluster = ClientJournalMapping.getClusterMapping(progress);
            double lX = sizeHandler.evRelativePosX(cluster.leftMost );
            double rX = sizeHandler.evRelativePosX(cluster.rightMost);
            double lY = sizeHandler.evRelativePosY(cluster.upperMost);
            double rY = sizeHandler.evRelativePosY(cluster.lowerMost);
            renderCluster(progress, cluster, lX, lY, rX, rY, zLevel);
        }
    }

    private void renderCluster(ResearchProgression p, ClientJournalMapping.JournalCluster cluster, double lowerPosX, double lowerPosY, double higherPosX, double higherPosY, float zLevel) {
        double scaledLeft = this.mousePointScaled.getScaledPosX() - sizeHandler.widthToBorder;
        double scaledTop =  this.mousePointScaled.getScaledPosY() - sizeHandler.heightToBorder;
        double xAdd = lowerPosX - scaledLeft;
        double yAdd = lowerPosY - scaledTop;
        double offsetX = realCoordLowerX + xAdd;
        double offsetY = realCoordLowerY + yAdd;

        GL11.glPushMatrix();
        GL11.glTranslated(offsetX, offsetY, 0);

        double width =  higherPosX - lowerPosX;
        double height = higherPosY - lowerPosY;

        Rectangle r = new Rectangle(MathHelper.floor_double(offsetX), MathHelper.floor_double(offsetY), MathHelper.floor_double(width), MathHelper.floor_double(height));
        clusterRectMap.put(r, p);

        Tessellator t = Tessellator.getInstance();
        VertexBuffer vb = t.getBuffer();
        cluster.cloudTexture.bind();

        double scale = sizeHandler.getScalingFactor();
        float br = 1F;
        if(scale > 8.01) {
            br = 0F;
        } else if (scale >= 6) {
            br = (float) (1F - ((scale - 6) / 2));
        }
        GL11.glColor4f(br, br, br, br);

        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_COLOR);

        vb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        vb.pos(0,     height, zLevel).tex(0, 1).endVertex();
        vb.pos(width, height, zLevel).tex(1, 1).endVertex();
        vb.pos(width, 0,      zLevel).tex(1, 0).endVertex();
        vb.pos(0,     0,      zLevel).tex(0, 0).endVertex();
        t.draw();

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glPopMatrix();
    }

    private void drawBlendedStarfieldLayers(double scalePosX, double scalePosY, float zLevel) {
        drawBlendedStarfieldOverlay(zLevel, scalePosX, scalePosY, 1.5);
        drawBlendedStarfieldOverlay(zLevel, scalePosX, scalePosY, 2.5);
        drawBlendedStarfieldOverlay(zLevel, scalePosX, scalePosY, 3.5);
    }

    private void drawClusterBackground(BindableResource tex, float zLevel) {
        double scale = sizeHandler.getScalingFactor();
        float br = 0F;
        if(scale > 8.01) {
            br = 1F;
        } else if (scale >= 6) {
            br = (float) (((scale - 6) / 2));
        }
        br *= 0.75F;
        GL11.glColor4f(br, br, br, br);
        tex.bind();

        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_COLOR);

        VertexBuffer vb = Tessellator.getInstance().getBuffer();
        vb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        vb.pos(realCoordLowerX,                   realCoordLowerY + realRenderHeight, zLevel).tex(0, 1).endVertex();
        vb.pos(realCoordLowerX + realRenderWidth, realCoordLowerY + realRenderHeight, zLevel).tex(1, 1).endVertex();
        vb.pos(realCoordLowerX + realRenderWidth, realCoordLowerY,                    zLevel).tex(1, 0).endVertex();
        vb.pos(realCoordLowerX,                   realCoordLowerY,                    zLevel).tex(0, 0).endVertex();
        Tessellator.getInstance().draw();

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glColor4f(1F, 1F, 1F, 1F);
    }

    private void drawBaseBackground(float zLevel) {
        float br = 0.5F;
        GL11.glColor4f(br, br, br, 1.0F);
        GL11.glDisable(GL11.GL_BLEND);
        textureResBack.bind();

        /*float lowU = (((float) this.leftOffset) - sizeHandler.widthToBorder) / ((float) sizeHandler.getTotalWidth());
        float highU = lowU + (((float) renderWidth) / ((float) sizeHandler.getTotalWidth()));
        float lowV = (((float) this.topOffset) - sizeHandler.heightToBorder) / ((float) sizeHandler.getTotalHeight());
        float highV = lowV + (((float) renderHeight) / ((float) sizeHandler.getTotalHeight()));*/

        VertexBuffer vb = Tessellator.getInstance().getBuffer();
        vb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        vb.pos(realCoordLowerX,                   realCoordLowerY + realRenderHeight, zLevel).tex(0, 1).endVertex();
        vb.pos(realCoordLowerX + realRenderWidth, realCoordLowerY + realRenderHeight, zLevel).tex(1, 1).endVertex();
        vb.pos(realCoordLowerX + realRenderWidth, realCoordLowerY,                    zLevel).tex(1, 0).endVertex();
        vb.pos(realCoordLowerX,                   realCoordLowerY,                    zLevel).tex(0, 0).endVertex();
        Tessellator.getInstance().draw();

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glColor4f(1F, 1F, 1F, 1F);
    }

    private void drawBlendedStarfieldOverlay(float zLevel, double scalePosX, double scalePosY, double scaleFactor) {
        GL11.glColor4f(0.8F, 0.8F, 0.8F, 0.4F);
        GL11.glPushMatrix();
        GL11.glScaled(scaleFactor, scaleFactor, scaleFactor);
        textureResOVL.bind();

        double th = sizeHandler.getTotalHeight() / sizeHandler.getScalingFactor();
        double tw = sizeHandler.getTotalWidth()  / sizeHandler.getScalingFactor();

        double lowU = (scalePosX - sizeHandler.widthToBorder) / tw;
        double highU = lowU + (((float) realRenderWidth) / tw);
        double lowV = (scalePosY - sizeHandler.heightToBorder) / th;
        double highV = lowV + (((float) realRenderHeight) / th);

        VertexBuffer vb = Tessellator.getInstance().getBuffer();
        vb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        vb.pos(0,               realRenderHeight, zLevel).tex(lowU,  highV).endVertex();
        vb.pos(realRenderWidth, realRenderHeight, zLevel).tex(highU, highV).endVertex();
        vb.pos(realRenderWidth, 0,                zLevel).tex(highU, lowV) .endVertex();
        vb.pos(0,               0,                zLevel).tex(lowU,  lowV) .endVertex();
        Tessellator.getInstance().draw();
        GL11.glPopMatrix();
        GL11.glColor4f(1F, 1F, 1F, 1F);
    }

}