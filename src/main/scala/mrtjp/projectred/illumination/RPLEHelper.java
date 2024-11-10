
package mrtjp.projectred.illumination;

import com.falsepattern.rple.api.common.color.DefaultColor;
import com.falsepattern.rple.api.common.color.LightValueColor;

public final class RPLEHelper {
    public static short getLightBrightnessColor(ILight light) {
        if (!light.isOn())
            return LightValueColor.LIGHT_VALUE_0.rgb16();
        int blockMeta = light.getColor();
        return DefaultColor.fromVanillaBlockMeta(blockMeta).rgb16();
    }
}
