package riskyken.armourersWorkshop.client.render.block;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import riskyken.armourersWorkshop.api.common.equipment.EnumEquipmentPart;
import riskyken.armourersWorkshop.api.common.equipment.EnumEquipmentType;
import riskyken.armourersWorkshop.client.model.armourer.ModelBlockArmourer;
import riskyken.armourersWorkshop.client.model.armourer.ModelChest;
import riskyken.armourersWorkshop.client.model.armourer.ModelFeet;
import riskyken.armourersWorkshop.client.model.armourer.ModelHand;
import riskyken.armourersWorkshop.client.model.armourer.ModelHead;
import riskyken.armourersWorkshop.client.model.armourer.ModelLegs;
import riskyken.armourersWorkshop.common.lib.LibModInfo;
import riskyken.armourersWorkshop.common.tileentities.TileEntityMiniArmourer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderBlockMiniArmourer extends TileEntitySpecialRenderer {
    
    private static final ResourceLocation guideImage = new ResourceLocation(LibModInfo.ID.toLowerCase(), "textures/blocks/guide.png");
    private static final ModelBlockArmourer modelArmourer = new ModelBlockArmourer();
    private static final ModelHead modelHead = new ModelHead();
    private static final ModelChest modelChest = new ModelChest();
    private static final ModelLegs modelLegs = new ModelLegs();
    private static final ModelFeet modelFeet = new ModelFeet();
    private static final ModelHand modelHand = new ModelHand();
    
    public void renderTileEntityAt(TileEntityMiniArmourer tileEntity, double x, double y, double z, float tickTime) {
        float scale = 0.0625F;
        GL11.glPushMatrix();
        GL11.glTranslated(x + 0.5D, y + 0.5D, z + 0.5D);
        GL11.glScalef(-1, -1, 1);
        modelArmourer.render(tileEntity, tickTime, scale);
        
        GL11.glScalef(2, 2, 2);
        GL11.glTranslatef(0, -8 * scale, 0);
        
        Minecraft mc = Minecraft.getMinecraft();
        mc.renderEngine.bindTexture(mc.thePlayer.getLocationSkin());
        
        
        EnumEquipmentType type = tileEntity.getEquipmentType();
        
        switch (type) {
        case NONE:
            break;
        case HEAD:
            GL11.glTranslated(0, -11 * scale, 0);
            modelHead.render(true);
            GL11.glTranslated(0, 11 * scale, 0);
            break;
        case CHEST:
            modelChest.renderChest();
            GL11.glTranslated(scale * 11, 0, 0);
            modelChest.renderLeftArm();
            GL11.glTranslated(scale * -22, 0, 0);
            modelChest.renderRightArm();
            break;
        case LEGS:
            GL11.glTranslated(scale * 6, 0, 0);
            modelLegs.renderLeftLeft();
            GL11.glTranslated(scale * -12, 0, 0);
            modelLegs.renderRightLeg();
            break;
        case SKIRT:
            GL11.glTranslated(scale * 2, 0, 0);
            modelLegs.renderLeftLeft();
            GL11.glTranslated(scale * -4, 0, 0);
            modelLegs.renderRightLeg();
            break;
        case FEET:
            GL11.glTranslated(scale * 6, 0, 0);
            modelFeet.renderLeftLeft();
            GL11.glTranslated(scale * -12, 0, 0);
            modelFeet.renderRightLeg();
            break;
        case SWORD:
            modelHand.render();
            break;
        case BOW:
            modelHand.render();
            break;
        }
        
        GL11.glPopMatrix();
        
        GL11.glPushMatrix();
        renderGuide(type, x + 0.5D, y + 0.5D, z + 0.5D, scale * 2);
        GL11.glPopMatrix();
    }
    
    private void renderGuide(EnumEquipmentType type, double x, double y, double z, float scale) {
        
        Minecraft.getMinecraft().getTextureManager().bindTexture(guideImage);
        int heightOffset = 1;
        
        switch (type) {
        case NONE:
            break;
        case HEAD:
            renderGuidePart(EnumEquipmentPart.HEAD, x, y + heightOffset, z, scale);
            break;
        case CHEST:
            renderGuidePart(EnumEquipmentPart.CHEST, x, y + heightOffset, z, scale);
            renderGuidePart(EnumEquipmentPart.LEFT_ARM, x, y + heightOffset, z, scale);
            renderGuidePart(EnumEquipmentPart.RIGHT_ARM, x, y + heightOffset, z, scale);
            break;
        case LEGS:
            renderGuidePart(EnumEquipmentPart.LEFT_LEG, x, y + heightOffset, z, scale);
            renderGuidePart(EnumEquipmentPart.RIGHT_LEG, x, y + heightOffset, z, scale);
            break;
        case SKIRT:
            renderGuidePart(EnumEquipmentPart.SKIRT, x, y + heightOffset, z, scale);
            break;
        case FEET:
            renderGuidePart(EnumEquipmentPart.LEFT_FOOT, x, y + heightOffset, z, scale);
            renderGuidePart(EnumEquipmentPart.RIGHT_FOOT, x, y + heightOffset, z, scale);
            break;
        case SWORD:
            renderGuidePart(EnumEquipmentPart.WEAPON, x, y + heightOffset, z, scale);
            break;
        case BOW:
            renderGuidePart(EnumEquipmentPart.BOW, x, y + heightOffset, z, scale);
            break;  
        }
    }
    
    private void renderGuidePart(EnumEquipmentPart part, double x, double y, double z, float scale) {
        GL11.glColor3f(1F, 1F, 1F);
        GL11.glPushMatrix();
        GL11.glTranslated(-part.xLocation * scale, part.yLocation * scale, -part.zLocation * scale);

        GL11.glDisable(GL11.GL_LIGHTING);
        
        renderGuideBox(x + part.getStartX() * scale,
                y + part.getStartY() * scale,
                z + part.getStartZ() * scale,
                part.getTotalXSize(), part.getTotalYSize(), part.getTotalZSize(), scale);
        /*
        renderGuideBox(x + (part.xSize / 2) + part.xOrigin - 0.5D,
                y - part.yOrigin - 0.5D,
                z - 0.5D, 1, 1, 1);
        */
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();
    }
    
    private void renderGuideBox(double x, double y, double z, int width, int height, int depth, float scale) {
        renderGuideFace(ForgeDirection.DOWN, x, y, z, width, depth, scale);
        renderGuideFace(ForgeDirection.UP, x, y + height * scale, z, width, depth, scale);
        renderGuideFace(ForgeDirection.EAST, x + width * scale, y, z, depth, height, scale);
        renderGuideFace(ForgeDirection.WEST, x, y, z, depth, height, scale);
        renderGuideFace(ForgeDirection.NORTH, x, y, z, width, height, scale);
        renderGuideFace(ForgeDirection.SOUTH, x, y, z + depth * scale, width, height, scale);
    }
    
    private void renderGuideFace(ForgeDirection dir, double x, double y, double z, double sizeX, double sizeY, float scale) {
        RenderManager renderManager = RenderManager.instance;
        Tessellator tessellator = Tessellator.instance;
        
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(0.5F, 0.5F, 0.5F, 0.5F);
        
        float scale1 = 0.999F;
        //GL11.glScalef(scale1, scale1, scale1);
        
        GL11.glTranslated(x, y, z);
        
        
        
        switch (dir) {
        case EAST:
            GL11.glRotated(-90, 0, 1, 0);
            break;
        case WEST:
            GL11.glRotated(-90, 0, 1, 0);
            break;
        case UP:
            GL11.glRotated(90, 1, 0, 0);
            break;
        case DOWN:
            GL11.glRotated(90, 1, 0, 0);
            break;
        default:
            break;
        }
        
        tessellator.setBrightness(15728880);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
        
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(0, 0, 0, 0, 0);
        tessellator.addVertexWithUV(0, sizeY * scale, 0, sizeY, 0);
        tessellator.addVertexWithUV(sizeX * scale, sizeY * scale, 0, sizeY, sizeX);
        tessellator.addVertexWithUV(sizeX * scale, 0, 0, 0, sizeX);
        tessellator.draw();
        
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1F, 1F, 1F, 1F);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glPopMatrix();
    }
    
    @Override
    public void renderTileEntityAt(TileEntity tileEntity, double x, double y, double z, float tickTime) {
        renderTileEntityAt((TileEntityMiniArmourer)tileEntity, x, y, z, tickTime);
    }
}
