package misq.jfx.components;

import com.jfoenix.controls.JFXToggleButton;
import com.jfoenix.skins.JFXToggleButtonSkin;
import javafx.scene.control.Skin;

import static misq.jfx.utils.TooltipUtil.showTooltipIfTruncated;

public class AutoTooltipSlideToggleButton extends JFXToggleButton {
    public AutoTooltipSlideToggleButton() {
        super();
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new AutoTooltipSlideToggleButtonSkin(this);
    }

    private class AutoTooltipSlideToggleButtonSkin extends JFXToggleButtonSkin {
        public AutoTooltipSlideToggleButtonSkin(JFXToggleButton toggleButton) {
            super(toggleButton);
        }

        @Override
        protected void layoutChildren(double x, double y, double w, double h) {
            super.layoutChildren(x, y, w, h);
            showTooltipIfTruncated(this, getSkinnable());
        }
    }
}
