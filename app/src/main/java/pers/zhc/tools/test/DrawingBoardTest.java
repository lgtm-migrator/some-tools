package pers.zhc.tools.test;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import androidx.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.floatingdrawing.PaintView;
import pers.zhc.tools.utils.DisplayUtil;

/**
 * @author bczhc
 */
public class DrawingBoardTest extends BaseActivity {

    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawing_board_test_activity);
        RelativeLayout layout = findViewById(R.id.rl);

        PaintView pv = new PaintView(this);
        pv.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        pv.setDrawingStrokeWidth(10F);
        pv.setDrawingColor(Color.RED);
        layout.addView(pv);
    }
}
