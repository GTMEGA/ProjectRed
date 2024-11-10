package mrtjp.projectred.illumination;

import com.falsepattern.rple.api.common.block.RPLECustomBlockBrightness;

import net.minecraft.world.IBlockAccess;
import cpw.mods.fml.common.Optional;

@Optional.Interface(
        iface = "com.falsepattern.rple.api.common.block.RPLECustomBlockBrightness",
        modid = "rple",
        striprefs = true
)
public interface ILight extends RPLECustomBlockBrightness {

    public boolean isOn();

    public int getColor();

    @Override
    default short rple$getCustomBrightnessColor() {
        return RPLEHelper.getLightBrightnessColor(this);
    }

    @Override
    default short rple$getCustomBrightnessColor(int blockMeta) {
        return RPLEHelper.getLightBrightnessColor(this);
    }

    @Override
    default short rple$getCustomBrightnessColor(IBlockAccess world, int blockMeta, int posX, int posY, int posZ) {
        return RPLEHelper.getLightBrightnessColor(this);
    }
}
