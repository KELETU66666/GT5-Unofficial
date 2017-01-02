package gregtech.common.tileentities.boilers;

import gregtech.api.enums.Dyes;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.objects.GT_RenderedTexture;
import gregtech.api.util.GT_ModHandler;
import gregtech.api.util.GT_OreDictUnificator;
import gregtech.api.util.GT_Utility;
import gregtech.common.GT_Pollution;
import gregtech.common.gui.GT_Container_Boiler;
import gregtech.common.gui.GT_GUIContainer_Boiler;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidStack;

public class GT_MetaTileEntity_Boiler_Lava extends GT_MetaTileEntity_Boiler {

    public GT_MetaTileEntity_Boiler_Lava(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional, "A Boiler running off Lava");
    }

    public GT_MetaTileEntity_Boiler_Lava(String aName, int aTier, String aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, aDescription, aTextures);
    }

    @Override
    public ITexture[][][] getTextureSet(ITexture[] aTextures) {
        ITexture[][][] rTextures = new ITexture[5][17][];
        for (byte i = -1; i < 16; i++) {
            rTextures[0][(i + 1)] = new ITexture[]{new GT_RenderedTexture(Textures.BlockIcons.MACHINE_STEELBRICKS_BOTTOM, Dyes.getModulation(i, Dyes._NULL.mRGBa))};
            rTextures[1][(i + 1)] = new ITexture[]{new GT_RenderedTexture(Textures.BlockIcons.MACHINE_STEELBRICKS_TOP, Dyes.getModulation(i, Dyes._NULL.mRGBa)), new GT_RenderedTexture(Textures.BlockIcons.OVERLAY_PIPE)};
            rTextures[2][(i + 1)] = new ITexture[]{new GT_RenderedTexture(Textures.BlockIcons.MACHINE_STEELBRICKS_SIDE, Dyes.getModulation(i, Dyes._NULL.mRGBa)), new GT_RenderedTexture(Textures.BlockIcons.OVERLAY_PIPE)};
            rTextures[3][(i + 1)] = new ITexture[]{new GT_RenderedTexture(Textures.BlockIcons.MACHINE_STEELBRICKS_SIDE, Dyes.getModulation(i, Dyes._NULL.mRGBa)), new GT_RenderedTexture(Textures.BlockIcons.BOILER_LAVA_FRONT)};
            rTextures[4][(i + 1)] = new ITexture[]{new GT_RenderedTexture(Textures.BlockIcons.MACHINE_STEELBRICKS_SIDE, Dyes.getModulation(i, Dyes._NULL.mRGBa)), new GT_RenderedTexture(Textures.BlockIcons.BOILER_LAVA_FRONT_ACTIVE)};
        }
        return rTextures;
    }

    @Override
    public int maxProgresstime() {
        return 1000;
    }

    @Override
    public Object getServerGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
        return new GT_Container_Boiler(aPlayerInventory, aBaseMetaTileEntity, 32000);
    }

    @Override
    public Object getClientGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
        return new GT_GUIContainer_Boiler(aPlayerInventory, aBaseMetaTileEntity, "SteelBoiler.png", 32000);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_Boiler_Lava(this.mName, this.mTier, this.mDescription, this.mTextures);
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        if ((aBaseMetaTileEntity.isServerSide()) && (aTick > 20L)) {
            if (this.mTemperature <= 20) {
                this.mTemperature = 20;
                this.mLossTimer = 0;
            }
            if (++this.mLossTimer > 20) {
                this.mTemperature -= 1;
                this.mLossTimer = 0;
            }
            if (getFluidAmount() != 0) {
                for (EnumFacing side : EnumFacing.VALUES) {
                    if (side.getIndex() != aBaseMetaTileEntity.getFrontFacing()) {
                        int drain = GT_Utility.fillFluidTank(
                                aBaseMetaTileEntity.getWorldObj(),
                                aBaseMetaTileEntity.getWorldPos(), side,
                                getDrainableStack());
                        if (drain != 0) {
                            drain(side, drain, true);
                            if (getFluidAmount() == 0)
                                break;
                        }
                    }
                }
            }
            if (aTick % 10L == 0L) {
                if (this.mTemperature > 100) {
                    if ((this.mFluid == null) || (!GT_ModHandler.isWater(this.mFluid)) || (this.mFluid.amount <= 0)) {
                        this.mHadNoWater = true;
                    } else {
                        if (this.mHadNoWater) {
                            aBaseMetaTileEntity.doExplosion(2048L);
                            return;
                        }
                        this.mFluid.amount -= 1;
                        if (this.mSteam == null) {
                            this.mSteam = GT_ModHandler.getSteam(300L);
                        } else if (GT_ModHandler.isSteam(this.mSteam)) {
                            this.mSteam.amount += 300;
                        } else {
                            this.mSteam = GT_ModHandler.getSteam(300L);
                        }
                    }
                } else {
                    this.mHadNoWater = false;
                }
            }
            if ((this.mSteam != null) &&
                    (this.mSteam.amount > 32000)) {
                sendSound((byte) 1);
                this.mSteam.amount = 24000;
            }
            if ((this.mProcessingEnergy <= 0) && (aBaseMetaTileEntity.isAllowedToWork()) &&
                    (GT_OreDictUnificator.isItemStackInstanceOf(this.mInventory[2], OrePrefixes.bucket.get(Materials.Lava)))) {
                this.mProcessingEnergy += 1000;
                aBaseMetaTileEntity.decrStackSize(2, 1);
                aBaseMetaTileEntity.addStackToSlot(3, GT_OreDictUnificator.get(OrePrefixes.bucket, Materials.Empty, 1L));
            }
            if ((this.mTemperature < 1000) && (this.mProcessingEnergy > 0) && (aTick % 8L == 0L)) {
                this.mProcessingEnergy -= 3;
                this.mTemperature += 1;
            }

            if(this.mProcessingEnergy>0 && (aTick % 20L == 0L)){
            	GT_Pollution.addPollution(getBaseMetaTileEntity().getWorldPos(), 20);
            }
            aBaseMetaTileEntity.setActive(this.mProcessingEnergy > 0);
        }
    }

    @Override
    public final int fill(FluidStack aFluid, boolean doFill) {
        if ((GT_ModHandler.isLava(aFluid)) && (this.mProcessingEnergy < 50)) {
            int tFilledAmount = Math.min(50, aFluid.amount);
            if (doFill) {
                this.mProcessingEnergy += tFilledAmount;
            }
            return tFilledAmount;
        }
        return super.fill(aFluid, doFill);
    }
}
